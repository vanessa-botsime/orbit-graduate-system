"""BRONZE -> SILVER: clean and structure the raw events.

Bronze is never edited. This step reads every *.jsonl file in data/bronze and writes:
  data/silver/events.parquet   clean, de-duplicated, typed events
  data/silver/rejected.jsonl   lines that failed validation, each with a reason

Rules
  - the line must be valid JSON with eventId, type, ownerId, applicationId, occurredAt
  - ids must be UUIDs, the timestamp must parse, the type must be a known Orbit type
  - the same eventId twice is kept once (Kafka and exports can repeat events)
  - STATUS_CHANGED details like "Acme - Dev: WISHLIST -> APPLIED" are split into columns

Limitation: Orbit's CREATED event does not carry a status, so new applications are
treated as WISHLIST (what the UI creates). Adding the status to that event is on the task board.
"""
import argparse
import json
import re
import uuid
from datetime import datetime, timezone
from pathlib import Path

import pandas as pd

VALID_TYPES = {
    "APPLICATION_CREATED", "APPLICATION_UPDATED", "APPLICATION_DELETED",
    "STATUS_CHANGED", "MATCH_SCORED", "RISK_CHECKED",
}
STAGES = ["WISHLIST", "DRAFTING", "APPLIED", "ASSESSMENT", "INTERVIEW", "OFFER", "REJECTED", "WITHDRAWN"]
REQUIRED = ["eventId", "type", "ownerId", "applicationId", "occurredAt"]
TRANSITION = re.compile(r"^(?P<label>.*): (?P<from>[A-Z_]+) -> (?P<to>[A-Z_]+)$")


def parse_timestamp(value):
    text = re.sub(r"(\.\d{6})\d+", r"\1", str(value).strip())      # Java can emit nanoseconds
    dt = datetime.fromisoformat(text.replace("Z", "+00:00"))
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    return dt.astimezone(timezone.utc)


def validate(raw):
    """Return (clean_record, None) or (None, reason)."""
    try:
        record = json.loads(raw)
    except json.JSONDecodeError:
        return None, "not_valid_json"
    if not isinstance(record, dict):
        return None, "not_a_json_object"
    missing = [k for k in REQUIRED if not record.get(k)]
    if missing:
        return None, "missing_" + "_".join(missing)
    try:
        event_id = str(uuid.UUID(str(record["eventId"])))
        owner_id = str(uuid.UUID(str(record["ownerId"])))
        application_id = str(uuid.UUID(str(record["applicationId"])))
    except ValueError:
        return None, "invalid_uuid"
    event_type = str(record["type"]).strip().upper()
    if event_type not in VALID_TYPES:
        return None, "unknown_event_type"
    try:
        occurred_at = parse_timestamp(record["occurredAt"])
    except ValueError:
        return None, "invalid_timestamp"

    detail = record.get("detail") or ""
    company = role = from_status = to_status = None
    if event_type == "STATUS_CHANGED":
        m = TRANSITION.match(detail)
        if not m or m["from"] not in STAGES or m["to"] not in STAGES:
            return None, "unparseable_status_change"
        from_status, to_status, label = m["from"], m["to"], m["label"]
    else:
        label = detail.split(": ")[0] if event_type in ("MATCH_SCORED", "RISK_CHECKED") else detail
    if " - " in label:
        company, role = (part.strip() for part in label.split(" - ", 1))
    elif label:
        company = label.strip()
    if event_type == "APPLICATION_CREATED":
        to_status = "WISHLIST"

    return {
        "event_id": event_id, "event_type": event_type, "owner_id": owner_id,
        "application_id": application_id, "company": company, "role": role,
        "from_status": from_status, "to_status": to_status, "detail": detail,
        "occurred_at": occurred_at,
    }, None


def run(data_dir="data"):
    base = Path(data_dir)
    bronze, silver = base / "bronze", base / "silver"
    silver.mkdir(parents=True, exist_ok=True)

    clean, rejected, seen = [], [], set()
    lines_read = duplicates = 0
    for path in sorted(bronze.glob("*.jsonl")):
        with path.open() as f:
            for line_no, raw in enumerate(f, start=1):
                raw = raw.strip()
                if not raw:
                    continue
                lines_read += 1
                record, reason = validate(raw)
                if reason:
                    rejected.append({"source_file": path.name, "line_no": line_no, "reason": reason, "raw": raw[:500]})
                elif record["event_id"] in seen:
                    duplicates += 1
                else:
                    seen.add(record["event_id"])
                    record["source_file"] = path.name
                    clean.append(record)

    columns = ["event_id", "event_type", "owner_id", "application_id", "company", "role",
               "from_status", "to_status", "detail", "occurred_at", "source_file"]
    df = pd.DataFrame(clean, columns=columns)
    if not df.empty:
        df["occurred_at"] = pd.to_datetime(df["occurred_at"], utc=True)
        df = df.sort_values("occurred_at").reset_index(drop=True)
    df.to_parquet(silver / "events.parquet", index=False)
    with (silver / "rejected.jsonl").open("w") as f:
        for r in rejected:
            f.write(json.dumps(r) + "\n")

    stats = {"lines_read": lines_read, "clean": len(df), "duplicates_dropped": duplicates, "rejected": len(rejected)}
    print("silver:", ", ".join(f"{k}={v}" for k, v in stats.items()))
    if rejected:
        reasons = pd.Series([r["reason"] for r in rejected]).value_counts().to_dict()
        print("rejected reasons:", reasons)
    return stats


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--data-dir", default="data")
    run(ap.parse_args().data_dir)


if __name__ == "__main__":
    main()
