#!/bin/bash
MODES=("redis" "local" "layered")

for MODE in "${MODES[@]}"; do
    echo "=== Mode: $MODE ==="

    CACHE_MODE=$MODE ./gradlew :apps:commerce-api:bootRun &
    APP_PID=$!

    until curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; do sleep 2; done

    # 워밍업 (10VU, 10초)
    k6 run --vus 10 --duration 10s k6/product-load-test.js 2>/dev/null

    # 본 테스트
    k6 run k6/product-load-test.js

    kill $APP_PID && wait $APP_PID 2>/dev/null
    redis-cli FLUSHALL
    echo ""
done
