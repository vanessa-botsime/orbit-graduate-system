"""GOLD -> dashboard: one self-contained HTML page (no libraries, no internet needed)."""
import argparse
import csv
import html
from pathlib import Path


def read(path):
    with Path(path).open(newline="") as f:
        return list(csv.DictReader(f))


def bars(rows, label_key, value_key, suffix=""):
    values = [float(r[value_key]) for r in rows] or [0]
    top = max(values) or 1
    out = []
    for r in rows:
        v = float(r[value_key])
        width = round(100 * v / top, 1)
        shown = f"{v:g}{suffix}"
        out.append(
            f'<div class="row"><span class="label">{html.escape(str(r[label_key]))}</span>'
            f'<span class="track"><span class="fill" style="width:{width}%"></span></span>'
            f'<span class="num">{shown}</span></div>'
        )
    return "\n".join(out) or '<p class="muted">No data yet.</p>'


def build(data_dir="data"):
    gold = Path(data_dir) / "gold"
    kpis = {r["kpi"]: r["value"] for r in read(gold / "kpis.csv")}
    median = kpis.get("median_days_to_reply") or ""
    cards = [
        ("Applications sent", kpis.get("applications_sent", "0")),
        ("Reply rate", f'{kpis.get("reply_rate_percent", "0")}%'),
        ("Median days to reply", f"{float(median):g}" if median else "n/a"),
        ("Interview rate", f'{kpis.get("interview_rate_percent", "0")}%'),
        ("Offers", kpis.get("offers", "0")),
    ]
    card_html = "".join(f'<div class="card"><div class="big">{html.escape(str(v))}</div>'
                        f'<div class="muted">{html.escape(k)}</div></div>' for k, v in cards)

    page = f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Orbit | Job search dashboard</title>
<style>
:root {{ --bg:#f2f4f8; --card:#fff; --ink:#1b2140; --muted:#5b6482; --line:#d8dde9; --accent:#3550e8; color-scheme:light; }}
@media (prefers-color-scheme: dark) {{ :root {{ --bg:#12162b; --card:#1b2140; --ink:#e8ebf7; --muted:#a3abc8; --line:#323a63; --accent:#8da0ff; color-scheme:dark; }} }}
body {{ margin:0; background:var(--bg); color:var(--ink); font:15px/1.5 system-ui,sans-serif; padding:1.5rem; }}
h1 {{ margin:0 0 .25rem; font-size:1.6rem; }} h2 {{ font-size:1.05rem; margin:0 0 .75rem; }}
.muted {{ color:var(--muted); font-size:.9rem; }}
.cards {{ display:grid; grid-template-columns:repeat(auto-fit,minmax(160px,1fr)); gap:.75rem; margin:1rem 0; }}
.card, section {{ background:var(--card); border:1px solid var(--line); border-radius:8px; padding:1rem; }}
.big {{ font-size:1.8rem; font-weight:700; color:var(--accent); }}
.grid {{ display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:.75rem; }}
.row {{ display:grid; grid-template-columns:110px 1fr 56px; gap:.5rem; align-items:center; margin:.3rem 0; }}
.track {{ background:var(--bg); border-radius:99px; height:12px; overflow:hidden; }}
.fill {{ display:block; height:100%; background:var(--accent); border-radius:99px; }}
.num {{ text-align:right; font-variant-numeric:tabular-nums; }}
</style></head><body>
<h1>Orbit job search dashboard</h1>
<p class="muted">Gold layer of the pipeline: bronze (raw events) &rarr; silver (cleaned) &rarr; gold (summaries).</p>
<div class="cards">{card_html}</div>
<div class="grid">
<section><h2>Where applications are now</h2>{bars(read(gold / "applications_by_stage.csv"), "stage", "applications")}</section>
<section><h2>Funnel (% of sent applications)</h2>{bars(read(gold / "funnel.csv"), "stage", "percent_of_applied", "%")}</section>
<section><h2>Applications added per week</h2>{bars(read(gold / "weekly_added.csv"), "week_start", "applications_added")}</section>
</div></body></html>"""
    out = gold / "dashboard.html"
    out.write_text(page, encoding="utf-8")
    print(f"dashboard: {out}")
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--data-dir", default="data")
    build(ap.parse_args().data_dir)


if __name__ == "__main__":
    main()
