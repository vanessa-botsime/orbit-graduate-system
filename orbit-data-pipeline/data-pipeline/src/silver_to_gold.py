"""SILVER -> GOLD: business-ready summary tables, built with SQL (DuckDB).

Reads data/silver/events.parquet and writes CSV + Parquet tables to data/gold:
  applications_by_stage   where each live application is right now
  weekly_added            applications added per week
  response_times          days from APPLIED to the first reply, per application
  funnel                  how many applications ever reached each stage
  kpis                    headline numbers for the dashboard

These queries are plain SQL, so the same logic runs in Athena on AWS later.
"""
import argparse
import csv
from pathlib import Path

import duckdb

STAGES = ["WISHLIST", "DRAFTING", "APPLIED", "ASSESSMENT", "INTERVIEW", "OFFER", "REJECTED", "WITHDRAWN"]
FUNNEL = ["APPLIED", "ASSESSMENT", "INTERVIEW", "OFFER", "REJECTED"]


def run(data_dir="data"):
    base = Path(data_dir)
    silver_file = base / "silver" / "events.parquet"
    gold = base / "gold"
    gold.mkdir(parents=True, exist_ok=True)

    con = duckdb.connect()
    con.execute(f"CREATE VIEW all_events AS SELECT * FROM read_parquet('{silver_file.as_posix()}')")
    # Deleted applications are left out of every table.
    con.execute("""
        CREATE VIEW events AS
        SELECT * FROM all_events
        WHERE application_id NOT IN (SELECT application_id FROM all_events WHERE event_type = 'APPLICATION_DELETED')
    """)

    def save(name, query):
        df = con.execute(query).df()
        df.to_csv(gold / f"{name}.csv", index=False)
        df.to_parquet(gold / f"{name}.parquet", index=False)
        return df

    stage_order = ", ".join(f"('{s}', {i})" for i, s in enumerate(STAGES))
    save("applications_by_stage", f"""
        WITH current AS (
            SELECT application_id, arg_max(to_status, occurred_at) AS stage
            FROM events WHERE to_status IS NOT NULL GROUP BY application_id
        ), order_ AS (SELECT * FROM (VALUES {stage_order}) AS t(stage, ord))
        SELECT o.stage, COUNT(c.application_id) AS applications
        FROM order_ o LEFT JOIN current c ON c.stage = o.stage
        GROUP BY o.stage, o.ord ORDER BY o.ord
    """)

    save("weekly_added", """
        SELECT CAST(date_trunc('week', occurred_at) AS DATE) AS week_start, COUNT(*) AS applications_added
        FROM events WHERE event_type = 'APPLICATION_CREATED'
        GROUP BY 1 ORDER BY 1
    """)

    response = save("response_times", """
        WITH applied AS (
            SELECT application_id, min(occurred_at) AS applied_at
            FROM events WHERE to_status = 'APPLIED' GROUP BY application_id
        ), reply AS (
            SELECT e.application_id, min(e.occurred_at) AS first_response_at
            FROM events e JOIN applied a ON a.application_id = e.application_id
            WHERE e.to_status IN ('ASSESSMENT', 'INTERVIEW', 'REJECTED', 'OFFER') AND e.occurred_at > a.applied_at
            GROUP BY e.application_id
        )
        SELECT a.application_id, a.applied_at, r.first_response_at,
               round(date_diff('second', a.applied_at, r.first_response_at) / 86400.0, 1) AS days_to_response
        FROM applied a LEFT JOIN reply r ON r.application_id = a.application_id
        ORDER BY a.applied_at
    """)

    reached = {
        row[0]: row[1] for row in con.execute(
            "SELECT to_status, COUNT(DISTINCT application_id) FROM events WHERE to_status IS NOT NULL GROUP BY to_status"
        ).fetchall()
    }
    applied = reached.get("APPLIED", 0)

    def pct(n):
        return round(100.0 * n / applied, 1) if applied else 0.0

    funnel_rows = [{"stage": s, "applications": reached.get(s, 0), "percent_of_applied": pct(reached.get(s, 0))}
                   for s in FUNNEL]
    import pandas as pd
    funnel = pd.DataFrame(funnel_rows)
    funnel.to_csv(gold / "funnel.csv", index=False)
    funnel.to_parquet(gold / "funnel.parquet", index=False)

    responded = int(response["days_to_response"].notna().sum())
    median_days = response["days_to_response"].median()
    total = con.execute(
        "SELECT COUNT(DISTINCT application_id) FROM events WHERE event_type = 'APPLICATION_CREATED'"
    ).fetchone()[0]
    kpis = {
        "applications_tracked": int(total),
        "applications_sent": int(applied),
        "got_a_reply": responded,
        "reply_rate_percent": pct(responded),
        "median_days_to_reply": "" if pd.isna(median_days) else round(float(median_days), 1),
        "interview_rate_percent": pct(reached.get("INTERVIEW", 0)),
        "rejection_rate_percent": pct(reached.get("REJECTED", 0)),
        "offers": int(reached.get("OFFER", 0)),
    }
    with (gold / "kpis.csv").open("w", newline="") as f:      # csv module keeps 44 as 44, not 44.0
        writer = csv.writer(f)
        writer.writerow(["kpi", "value"])
        writer.writerows(kpis.items())
    print("gold:", ", ".join(f"{k}={v}" for k, v in kpis.items()))
    return kpis


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--data-dir", default="data")
    run(ap.parse_args().data_dir)


if __name__ == "__main__":
    main()
