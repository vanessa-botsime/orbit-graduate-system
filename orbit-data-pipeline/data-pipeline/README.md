# Orbit data pipeline (bronze / silver / gold)

Turns the events Orbit already produces (an application was created, moved to a new stage, scored,
deleted) into clean tables and a small dashboard that answers: *how is my job search going?*

```
Orbit API --> Kafka --> audit_logs (Postgres)
                              |  export_from_postgres.py   (or generate_demo_events.py)
                              v
   BRONZE  data/bronze/*.jsonl      raw events, exactly as received, never edited
                              |  bronze_to_silver.py
                              v
   SILVER  data/silver/events.parquet   validated, de-duplicated, typed  (+ rejected.jsonl)
                              |  silver_to_gold.py   (SQL in DuckDB)
                              v
   GOLD    data/gold/*.csv|parquet      summary tables  -->  build_dashboard.py  -->  dashboard.html
```

| Layer | What it holds | Why it exists |
|---|---|---|
| Bronze | Raw JSON lines | Source of truth. If a rule is wrong later, rebuild everything from here |
| Silver | One clean row per event | Bad lines are quarantined with a reason, duplicates dropped, status changes split into columns |
| Gold | Answers: stage counts, funnel, reply rate, days to reply | What a dashboard or an analyst actually reads |

## Run it (no cloud, no Docker needed)

```bash
cd data-pipeline
python3 -m venv .venv && source .venv/bin/activate
make install
make demo          # fake events -> bronze -> silver -> gold -> dashboard
make test
```

Open `data/gold/dashboard.html` in a browser. Every step also works on its own:
`python3 src/bronze_to_silver.py --data-dir data`.

### Use your own Orbit data (locally only)

```bash
docker compose up -d                  # from the repo root
export DB_PASSWORD=<POSTGRES_PASSWORD from your .env>
cd data-pipeline && make export && make pipeline
```

This exports your real activity. **Keep it on your laptop.** Use the generated demo data for anything in the cloud.

## What the silver step checks

Valid JSON; `eventId`, `type`, `ownerId`, `applicationId`, `occurredAt` present; ids are UUIDs; the
timestamp parses; the type is a known Orbit event; the same `eventId` is kept once. Everything else goes to
`data/silver/rejected.jsonl` with a reason (`not_valid_json`, `invalid_uuid`, `invalid_timestamp`, ...).

## Gold tables

| File | Question it answers |
|---|---|
| `applications_by_stage` | Where is each live application right now? |
| `funnel` | What share of sent applications reached assessment, interview, offer, rejection? |
| `response_times` | How many days from "applied" to the first reply, per application? |
| `weekly_added` | How many applications did I add each week? |
| `kpis` | Headline numbers: sent, reply rate, median days to reply, interview rate, offers |

Deleted applications are excluded everywhere.

## Moving it to AWS (next step)

| Local | AWS |
|---|---|
| `data/bronze`, `silver`, `gold` folders | One S3 bucket with `bronze/`, `silver/`, `gold/` prefixes |
| `bronze_to_silver.py` | The same logic as an AWS Glue Python job (or a small script run on a schedule) |
| DuckDB SQL in `silver_to_gold.py` | The same SQL as Athena queries over the Parquet files |
| `dashboard.html` | Same page, or a chart over the gold tables |

Start with S3 and Athena on one file, then add Glue. Set an AWS budget alert first, and upload demo data only.

## Known limitations

- Orbit's `APPLICATION_CREATED` event has no status, so new applications count as WISHLIST (what the UI creates). Adding the status to that event is a task for the board.
- "First reply" means the first move to assessment, interview, rejection or offer after applying. It does not read emails.
- Events are exported in batches, not streamed. A streaming consumer is a possible later step.
- Statuses come from parsing the event's text. A structured event payload would be more robust.
