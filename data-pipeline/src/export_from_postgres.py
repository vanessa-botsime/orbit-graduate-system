"""Export Orbit's audit log from your LOCAL Postgres into bronze JSONL.

Needs the stack running (docker compose up) and DB_PASSWORD set to the
POSTGRES_PASSWORD from your .env. Postgres is published on localhost:5432.

PRIVACY: this exports your real activity. Do not upload it to a cloud account.
For cloud demos, use generate_demo_events.py instead.
"""
import argparse
import json
import os
from datetime import datetime, timezone
from pathlib import Path

import psycopg2


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--data-dir", default="data")
    args = ap.parse_args()

    password = os.environ.get("DB_PASSWORD")
    if not password:
        raise SystemExit("Set DB_PASSWORD (the POSTGRES_PASSWORD from your .env) first.")

    conn = psycopg2.connect(
        host=os.environ.get("DB_HOST", "localhost"),
        port=int(os.environ.get("DB_PORT", "5432")),
        dbname=os.environ.get("DB_NAME", "orbit"),
        user=os.environ.get("DB_USER", "orbit"),
        password=password,
    )
    out_dir = Path(args.data_dir) / "bronze"
    out_dir.mkdir(parents=True, exist_ok=True)
    out = out_dir / f"export_{datetime.now(timezone.utc):%Y%m%dT%H%M%S}.jsonl"

    count = 0
    with conn, conn.cursor() as cur, out.open("w") as f:
        cur.execute("""SELECT event_id, event_type, owner_id, application_id, detail, occurred_at
                       FROM audit_logs ORDER BY occurred_at""")
        for event_id, etype, owner, app, detail, occurred in cur:
            f.write(json.dumps({
                "eventId": str(event_id), "type": etype, "ownerId": str(owner),
                "applicationId": str(app) if app else None, "detail": detail,
                "occurredAt": occurred.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f") + "Z",
            }) + "\n")
            count += 1
    print(f"Exported {count} events to {out}")


if __name__ == "__main__":
    main()
