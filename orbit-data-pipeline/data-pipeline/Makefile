# Run from the data-pipeline/ folder.
PY ?= python3

install:
	$(PY) -m pip install -r requirements.txt

demo:            ## fake events -> bronze -> silver -> gold -> dashboard
	$(PY) src/generate_demo_events.py
	$(PY) src/bronze_to_silver.py
	$(PY) src/silver_to_gold.py
	$(PY) src/build_dashboard.py
	@echo "Open data/gold/dashboard.html in your browser"

export:          ## real events from your local Orbit database -> bronze
	$(PY) src/export_from_postgres.py

pipeline:        ## re-run the three steps on whatever is in data/bronze
	$(PY) src/bronze_to_silver.py
	$(PY) src/silver_to_gold.py
	$(PY) src/build_dashboard.py

test:
	$(PY) -m pytest -q

clean:
	rm -rf data/bronze/*.jsonl data/silver data/gold
