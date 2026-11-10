"""Create fake Orbit events (the BRONZE input) for a demo account.

The data is deliberately a bit messy, like real exports are: duplicate events,
broken JSON lines, missing fields, bad timestamps and an unknown event type.
That gives the silver step something real to clean.

Use demo data in the cloud, never your real job-search data.
"""
import argparse
import json
import random
import uuid
from datetime import datetime, timedelta, timezone
from pathlib import Path

COMPANIES = [
    "Acme Retail", "Bluepeak Bank", "Cobalt Logistics", "Delta Insure", "Evergreen Telecom",
    "Fathom Health", "Granite Mining Tech", "Harbour Payments", "Ionic Energy", "Jacaranda Software",
    "Kestrel Analytics", "Lumen Mobility", "Marula Systems", "Nimbus Cloud", "Orchid Pharma",
]
ROLES = ["Graduate Developer", "Junior Backend Engineer", "Systems Integration Intern",
         "Cloud Support Engineer", "Data Engineering Intern", "Software Engineer (Graduate)"]


def make_event(rng, type_, owner, app, detail, ts):
    return {
        "eventId": str(uuid.UUID(int=rng.getrandbits(128), version=4)),
        "type": type_,
        "ownerId": owner,
        "applicationId": app,
        "detail": detail,
        "occurredAt": ts.strftime("%Y-%m-%dT%H:%M:%S.%f") + "Z",
    }


def build_events(seed=42, n_apps=45, weeks=10, now=None):
    rng = random.Random(seed)
    now = now or datetime.now(timezone.utc)
    owner = str(uuid.UUID(int=rng.getrandbits(128), version=4))
    events = []
    span_days = weeks * 7

    for _ in range(n_apps):
        app = str(uuid.UUID(int=rng.getrandbits(128), version=4))
        label = f"{rng.choice(COMPANIES)} - {rng.choice(ROLES)}"
        t = now - timedelta(days=span_days) + timedelta(days=rng.uniform(0, span_days - 10))
        status = "WISHLIST"
        mine = [make_event(rng, "APPLICATION_CREATED", owner, app, label, t)]

        def move(new, lo, hi):
            nonlocal t, status
            t = t + timedelta(days=rng.uniform(lo, hi))
            mine.append(make_event(rng, "STATUS_CHANGED", owner, app, f"{label}: {status} -> {new}", t))
            status = new

        if rng.random() < 0.5:
            mine.append(make_event(rng, "MATCH_SCORED", owner, app, f"{label}: {rng.randint(35, 90)}%",
                                   t + timedelta(hours=1)))
        if rng.random() < 0.3:
            mine.append(make_event(rng, "RISK_CHECKED", owner, app, f"{label}: {rng.choice(['LOW', 'LOW', 'MEDIUM'])}",
                                   t + timedelta(hours=2)))
        if rng.random() < 0.7:
            move("DRAFTING", 0.1, 2)
        if rng.random() > 0.12:                      # most applications do get sent
            move("APPLIED", 0.2, 6)
            roll = rng.random()
            if roll < 0.40:
                pass                                  # silence: no reply yet
            elif roll < 0.70:
                move("REJECTED", 4, 20)
            else:
                move("ASSESSMENT", 3, 12)
                if rng.random() < 0.4:
                    move("REJECTED", 2, 10)
                else:
                    move("INTERVIEW", 4, 14)
                    r2 = rng.random()
                    if r2 < 0.3:
                        move("OFFER", 3, 10)
                    elif r2 < 0.6:
                        move("REJECTED", 2, 8)
        if rng.random() < 0.04:
            mine.append(make_event(rng, "APPLICATION_DELETED", owner, app, label, t + timedelta(hours=3)))
        events.extend(e for e in mine if datetime.fromisoformat(e["occurredAt"].replace("Z", "+00:00")) <= now)

    events.sort(key=lambda e: e["occurredAt"])
    return events, owner


def messy_lines(rng, events, owner):
    """Turn events into JSON lines and add realistic problems."""
    lines = [json.dumps(e) for e in events]
    dupes = [json.dumps(e) for e in rng.sample(events, max(1, len(events) // 20))]
    some = events[len(events) // 2]
    bad = [
        "this line is not JSON at all",
        json.dumps({**some, "eventId": "not-a-uuid"}),
        json.dumps({k: v for k, v in some.items() if k != "ownerId"}),
        json.dumps({**some, "eventId": str(uuid.uuid4()), "occurredAt": "yesterday-ish"}),
        json.dumps({**some, "eventId": str(uuid.uuid4()), "type": "APPLICATION_TELEPORTED"}),
    ]
    return lines, dupes, bad


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--data-dir", default="data")
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--applications", type=int, default=45)
    args = ap.parse_args()

    rng = random.Random(args.seed + 1)
    events, owner = build_events(seed=args.seed, n_apps=args.applications)
    lines, dupes, bad = messy_lines(rng, events, owner)

    bronze = Path(args.data_dir) / "bronze"
    bronze.mkdir(parents=True, exist_ok=True)
    half = len(lines) // 2
    (bronze / "events_part1.jsonl").write_text("\n".join(lines[:half] + bad[:3]) + "\n")
    # part 2 repeats some events from part 1, as overlapping exports do
    (bronze / "events_part2.jsonl").write_text("\n".join(lines[half:] + dupes + bad[3:]) + "\n")
    print(f"Wrote {len(lines)} good events, {len(dupes)} duplicates and {len(bad)} bad lines to {bronze}")


if __name__ == "__main__":
    main()
