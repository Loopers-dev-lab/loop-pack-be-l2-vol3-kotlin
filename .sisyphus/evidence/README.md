# Round 5 Evidence

- `task-t1-seed-summary.txt`: 100k+ seed run summary, brand distribution target, and dataset label.
- `task-t4-explain.txt`: `EXPLAIN` output comparing the baseline query (`IGNORE INDEX`) and the optimized query.
- `task-t4-performance.txt`: average query latency comparison for before/after measurements.

Generate or refresh these files with:

```bash
./gradlew :apps:commerce-api:bootRun --args='--round5.read-optimization.enabled=true --round5.read-optimization.dataset-label=round5'
```

Optional overrides:

- `--round5.read-optimization.total-products=100000`
- `--round5.read-optimization.brand-count=50`
- `--round5.read-optimization.batch-size=1000`
- `--round5.read-optimization.measurement-runs=15`
- `--round5.read-optimization.target-brand-index=0`
- `--round5.read-optimization.evidence-dir=.sisyphus/evidence`
