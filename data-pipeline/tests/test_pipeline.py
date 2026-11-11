import json
import uuid

import pandas as pd

import bronze_to_silver
import silver_to_gold

OWNER = str(uuid.uuid4())


def ev(type_, app, detail, ts, event_id=None):
    return {"eventId": event_id or str(uuid.uuid4()), "type": type_, "ownerId": OWNER,
            "applicationId": app, "detail": detail, "occurredAt": ts}


def write_bronze(tmp_path, lines, name="events.jsonl"):
    (tmp_path / "bronze").mkdir(parents=True, exist_ok=True)
    (tmp_path / "bronze" / name).write_text(
        "\n".join(l if isinstance(l, str) else json.dumps(l) for l in lines) + "\n")


def test_silver_cleans_dedupes_and_quarantines(tmp_path):
    app = str(uuid.uuid4())
    good = ev("APPLICATION_CREATED", app, "Acme - Dev", "2026-11-01T09:00:00.123456789Z")
    write_bronze(tmp_path, [
        good, good,                                               # duplicate event id
        "not json at all",
        {**good, "eventId": str(uuid.uuid4()), "ownerId": ""},   # missing owner
        {**good, "eventId": "nope"},                              # bad uuid
        {**good, "eventId": str(uuid.uuid4()), "occurredAt": "soon"},
        {**good, "eventId": str(uuid.uuid4()), "type": "TELEPORT"},
    ])
    stats = bronze_to_silver.run(tmp_path)
    assert stats == {"lines_read": 7, "clean": 1, "duplicates_dropped": 1, "rejected": 5}
    df = pd.read_parquet(tmp_path / "silver" / "events.parquet")
    assert df.loc[0, "company"] == "Acme" and df.loc[0, "role"] == "Dev"
    assert df.loc[0, "to_status"] == "WISHLIST"
    reasons = {json.loads(l)["reason"] for l in (tmp_path / "silver" / "rejected.jsonl").read_text().splitlines()}
    assert reasons == {"not_valid_json", "missing_ownerId", "invalid_uuid", "invalid_timestamp", "unknown_event_type"}


def test_status_change_is_split_into_columns(tmp_path):
    app = str(uuid.uuid4())
    write_bronze(tmp_path, [ev("STATUS_CHANGED", app, "Acme - Dev: DRAFTING -> APPLIED", "2026-11-02T09:00:00Z")])
    bronze_to_silver.run(tmp_path)
    row = pd.read_parquet(tmp_path / "silver" / "events.parquet").iloc[0]
    assert (row["from_status"], row["to_status"], row["company"]) == ("DRAFTING", "APPLIED", "Acme")


def test_gold_funnel_response_time_and_deleted_apps(tmp_path):
    a, b, c, gone = (str(uuid.uuid4()) for _ in range(4))
    events = [
        # a: sent, interview after 4 days, then offer
        ev("APPLICATION_CREATED", a, "A - Dev", "2026-11-01T08:00:00Z"),
        ev("STATUS_CHANGED", a, "A - Dev: WISHLIST -> APPLIED", "2026-11-01T10:00:00Z"),
        ev("STATUS_CHANGED", a, "A - Dev: APPLIED -> INTERVIEW", "2026-11-05T10:00:00Z"),
        ev("STATUS_CHANGED", a, "A - Dev: INTERVIEW -> OFFER", "2026-11-08T10:00:00Z"),
        # b: sent, rejected after 2 days
        ev("APPLICATION_CREATED", b, "B - Dev", "2026-11-02T08:00:00Z"),
        ev("STATUS_CHANGED", b, "B - Dev: WISHLIST -> APPLIED", "2026-11-02T10:00:00Z"),
        ev("STATUS_CHANGED", b, "B - Dev: APPLIED -> REJECTED", "2026-11-04T10:00:00Z"),
        # c: sent, no reply
        ev("APPLICATION_CREATED", c, "C - Dev", "2026-11-03T08:00:00Z"),
        ev("STATUS_CHANGED", c, "C - Dev: WISHLIST -> APPLIED", "2026-11-03T10:00:00Z"),
        # gone: sent then deleted, must not count anywhere
        ev("APPLICATION_CREATED", gone, "D - Dev", "2026-11-03T08:00:00Z"),
        ev("STATUS_CHANGED", gone, "D - Dev: WISHLIST -> APPLIED", "2026-11-03T09:00:00Z"),
        ev("APPLICATION_DELETED", gone, "D - Dev", "2026-11-03T11:00:00Z"),
    ]
    write_bronze(tmp_path, events)
    bronze_to_silver.run(tmp_path)
    kpis = silver_to_gold.run(tmp_path)

    assert kpis["applications_tracked"] == 3
    assert kpis["applications_sent"] == 3
    assert kpis["got_a_reply"] == 2
    assert kpis["reply_rate_percent"] == 66.7
    assert kpis["median_days_to_reply"] == 3.0          # 4 days and 2 days
    assert kpis["offers"] == 1

    stage = pd.read_csv(tmp_path / "gold" / "applications_by_stage.csv").set_index("stage")["applications"]
    assert stage["OFFER"] == 1 and stage["REJECTED"] == 1 and stage["APPLIED"] == 1 and stage["WISHLIST"] == 0
    funnel = pd.read_csv(tmp_path / "gold" / "funnel.csv").set_index("stage")
    assert funnel.loc["INTERVIEW", "applications"] == 1
    assert funnel.loc["APPLIED", "percent_of_applied"] == 100.0
