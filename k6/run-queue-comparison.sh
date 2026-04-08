#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
RESULT_DIR="$ROOT_DIR/k6/results"
APP_LOG_DIR="$RESULT_DIR/app-logs"
K6_IMAGE="grafana/k6:latest"
STRATEGIES=(REDIS_ONLY REDIS_KAFKA KAFKA_ONLY PESSIMISTIC_LOCK DISTRIBUTED_LOCK)

export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"

mkdir -p "$RESULT_DIR" "$APP_LOG_DIR"

cleanup() {
  if [[ -n "${APP_PID:-}" ]] && kill -0 "$APP_PID" >/dev/null 2>&1; then
    kill "$APP_PID" >/dev/null 2>&1 || true
    wait "$APP_PID" >/dev/null 2>&1 || true
  fi
}

trap cleanup EXIT

wait_for_app() {
  local attempts=0
  until curl -fsS "http://localhost:8081/actuator/health" >/dev/null 2>&1; do
    attempts=$((attempts + 1))
    if [[ "$attempts" -ge 120 ]]; then
      echo "application failed to become healthy" >&2
      return 1
    fi
    sleep 2
  done
}

run_strategy() {
  local strategy="$1"
  local strategy_slug
  strategy_slug="$(printf '%s' "$strategy" | tr '[:upper:]' '[:lower:]')"
  local run_id="${strategy_slug}-$(date +%Y%m%d%H%M%S)"
  local app_log="$APP_LOG_DIR/${strategy}.log"
  local summary_file="$RESULT_DIR/${strategy}.summary.json"

  echo "=== Running strategy: $strategy ==="
  cleanup

  ./gradlew :apps:commerce-api:bootRun \
    --args="--queue.experiment.enforce-order-gate=true --queue.experiment.scheduler.enabled=true --queue.experiment.batch-size-override=10 --queue.experiment.active-strategy=$strategy" \
    >"$app_log" 2>&1 &
  APP_PID=$!

  wait_for_app

  docker run --rm \
    -v "$ROOT_DIR/k6:/scripts" \
    "$K6_IMAGE" run \
    --summary-export "/scripts/results/${strategy}.summary.json" \
    -e BASE_URL="http://host.docker.internal:8080" \
    -e QUEUE_STRATEGY="$strategy" \
    -e RUN_ID="$run_id" \
    -e USER_COUNT="40" \
    -e PRODUCT_COUNT="5" \
    /scripts/scripts/queue-strategy-comparison.js

  if [[ ! -f "$summary_file" ]]; then
    echo "summary file not created for $strategy" >&2
    return 1
  fi

  cleanup
  APP_PID=""
}

cd "$ROOT_DIR"

for strategy in "${STRATEGIES[@]}"; do
  run_strategy "$strategy"
done

python3 "$ROOT_DIR/k6/scripts/queue-comparison-report.py" "$RESULT_DIR"/*.summary.json \
  | tee "$RESULT_DIR/comparison-table.md"

echo "Results saved under $RESULT_DIR"
