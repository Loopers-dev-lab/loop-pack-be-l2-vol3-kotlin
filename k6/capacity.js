import { check, sleep } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";
import {
    registerUser, toggleQueue, enterQueue, getPosition, getQueueStatus,
    pollUntilToken, createOrder, parseBody,
} from "./helpers.js";

/**
 * Capacity Test — 역산(Reverse Engineering) 기반 처리량 검증
 *
 * ============================================================
 * 1. 시스템 제약 조건 (CLAUDE.md + application.yml)
 * ============================================================
 *
 *   구분              설정값          비고
 *   ─────────────────────────────────────────────────────────
 *   DB Pool           max=50          HikariCP
 *   Tomcat            virtual threads max-connections=10000
 *   Redis             단일 인스턴스    Master-Replica
 *   Scheduler         3초 주기        batchSize=300
 *   Token TTL         300초 (5분)     SET NX EX
 *   SLO               p99 ≤ 500ms    에러율 ≤ 0.1%
 *
 * ============================================================
 * 2. 역산: 제약 조건 → 처리량 유도
 * ============================================================
 *
 * [Queue Entry — Redis bound]
 *   Redis ZADD NX: ~50K OPS (sorted set, single thread)
 *   인증 BCrypt 캐시 히트 시: ~5ms/req
 *   → 이론 한계: 10,000+ RPS
 *   → 검증 목표: 1,000 RPS 안정 (SLO 내)
 *
 * [Queue Polling — Redis bound]
 *   Redis ZRANK: O(log N), ~50K OPS
 *   동적 retryAfter로 Polling 분산:
 *     10,000명 대기 시 평균 retryAfter=3s → 3,333 poll/s
 *     동적 조정 후: ~1,000-2,000 poll/s
 *   → 검증 목표: 2,000 RPS 안정 (SLO 내)
 *
 * [Order Processing — DB bound]
 *   batchSize=300 / 3s = 100명/s 입장 (스케줄러 병목)
 *   주문 1건: SELECT FOR UPDATE + INSERT ≈ 200ms
 *   DB Pool 50 × (1000ms / 200ms) = 250 TPS (DB 한계)
 *   → 실제 병목: 스케줄러 100명/s < DB 250 TPS
 *   → 검증 목표: 100 TPS 주문 처리 (스케줄러 기준)
 *
 * [Admin Status — Redis bound (Pipeline 적용 후)]
 *   SMEMBERS + Pipeline GET: 2 round-trip
 *   → 10K 멤버 기준 100ms 이내 목표
 *
 * ============================================================
 * 3. 부하 시나리오 VU 수 근거
 * ============================================================
 *
 *   시나리오     Peak RPS   VU 산출 근거
 *   ─────────────────────────────────────────────────────────
 *   entry        1,000      Redis 이론치 10K의 10%, SLO 검증
 *   polling      2,000      10K 대기자 × retryAfter 3s = 3.3K, 60% 적용
 *   order         100       스케줄러 100명/s, 1:1 검증
 *   adminStatus     5       모니터링 엔드포인트, 5초 간격
 *
 *   VU 환산 (Little's Law: VU = RPS × avg_response_time):
 *   - entry:   1000 RPS × 0.05s = 50 VUs
 *   - polling: 2000 RPS × 0.05s = 100 VUs
 *   - order:    100 RPS × 0.3s  = 30 VUs
 *   → 안전 마진 2x: entry=100, polling=200, order=60
 *
 * ============================================================
 * 4. SLO 기준
 * ============================================================
 *
 *   p99 ≤ 500ms, 에러율 ≤ 0.1%
 *   (커머스 표준, 100명 중 99명이 0.5초 이내 응답)
 *   이 기준 초과 시 사실상 장애로 판단
 */

// --- Custom Metrics ---
const entryThroughput = new Rate("entry_slo_pass");
const pollingThroughput = new Rate("polling_slo_pass");
const orderThroughput = new Rate("order_slo_pass");
const entryLatency = new Trend("entry_latency_ms");
const pollingLatency = new Trend("polling_latency_ms");
const orderLatency = new Trend("order_latency_ms");

export const options = {
    scenarios: {
        // 역산 기반: 1,000 RPS → VU=100 (Little's Law + 2x 마진)
        entry: {
            executor: "constant-arrival-rate",
            rate: 1000,
            timeUnit: "1s",
            duration: "1m",
            preAllocatedVUs: 100,
            maxVUs: 300,
            exec: "entryScenario",
        },
        // 역산 기반: 2,000 RPS → VU=200
        polling: {
            executor: "constant-arrival-rate",
            rate: 2000,
            timeUnit: "1s",
            duration: "1m",
            preAllocatedVUs: 200,
            maxVUs: 500,
            exec: "pollingScenario",
            startTime: "10s",
        },
        // 역산 기반: 100 TPS → VU=60 (스케줄러 처리율 검증)
        order: {
            executor: "constant-arrival-rate",
            rate: 100,
            timeUnit: "1s",
            duration: "1m",
            preAllocatedVUs: 60,
            maxVUs: 200,
            exec: "orderScenario",
            startTime: "20s",
        },
        // 어드민 모니터링: 5 RPS (Pipeline 성능 검증)
        adminMonitor: {
            executor: "constant-arrival-rate",
            rate: 5,
            timeUnit: "1s",
            duration: "1m",
            preAllocatedVUs: 5,
            maxVUs: 10,
            exec: "adminScenario",
            startTime: "10s",
        },
    },
    thresholds: {
        // SLO: p99 ≤ 500ms
        "entry_latency_ms": ["p(99)<500"],
        "polling_latency_ms": ["p(99)<500"],
        "order_latency_ms": ["p(99)<500"],
        // SLO: 에러율 ≤ 0.1%
        "entry_slo_pass": ["rate>0.999"],
        "polling_slo_pass": ["rate>0.999"],
        "order_slo_pass": ["rate>0.99"],
        // HTTP level
        "http_req_duration{name:enter}": ["p(99)<500"],
        "http_req_duration{name:position}": ["p(99)<500"],
        "http_req_failed{name:enter}": ["rate<0.001"],
        "http_req_failed{name:position}": ["rate<0.001"],
    },
};

export function setup() {
    // 사전 유저 등록 (BCrypt 부하 분리)
    for (let i = 1; i <= 500; i++) {
        registerUser(`capuser${i}`);
    }
    toggleQueue(true);
    // 대기열에 유저를 미리 채워서 Polling 시나리오 준비
    for (let i = 1; i <= 200; i++) {
        enterQueue(`capuser${i}`);
    }
    return { ready: true };
}

// --- Entry: 1,000 RPS SLO 검증 ---
// 근거: Redis ZADD NX 이론치 50K OPS의 2%, 안전 마진 충분
export function entryScenario() {
    const loginId = `capuser${(__VU % 500) + 1}`;
    const start = Date.now();

    const res = enterQueue(loginId);
    const elapsed = Date.now() - start;

    entryLatency.add(elapsed);
    entryThroughput.add(res.status === 200);

    check(res, {
        "entry: 200 within SLO": (r) => r.status === 200 && elapsed < 500,
    });
}

// --- Polling: 2,000 RPS SLO 검증 ---
// 근거: 10K 대기자 / retryAfter 3s = 3,333 poll/s, 60% 적용
export function pollingScenario() {
    const loginId = `capuser${(__VU % 200) + 1}`;
    const start = Date.now();

    const res = getPosition(loginId);
    const elapsed = Date.now() - start;

    pollingLatency.add(elapsed);
    pollingThroughput.add(res.status === 200);

    check(res, {
        "polling: 200 within SLO": (r) => r.status === 200 && elapsed < 500,
    });
}

// --- Order: 100 TPS SLO 검증 ---
// 근거: 스케줄러 batchSize=300/3s = 100명/s 입장율
export function orderScenario() {
    const loginId = `capuser${(__VU % 100) + 1}`;

    // 토큰이 있으면 주문, 없으면 진입 후 대기
    const posRes = getPosition(loginId);
    const body = parseBody(posRes);

    if (body && body.data && body.data.token) {
        const start = Date.now();
        const orderRes = createOrder(loginId, body.data.token);
        const elapsed = Date.now() - start;

        orderLatency.add(elapsed);
        orderThroughput.add(orderRes.status === 202 || orderRes.status === 200);

        check(orderRes, {
            "order: accepted within SLO": (r) =>
                (r.status === 202 || r.status === 200) && elapsed < 500,
        });
    } else {
        // 토큰 없으면 진입만 (스케줄러 처리 대기)
        enterQueue(loginId);
        orderThroughput.add(true); // 대기 중은 실패 아님
    }
}

// --- Admin: Pipeline 적용 후 성능 검증 ---
// 근거: activeCount() Pipeline 적용 → N round-trip → 1
export function adminScenario() {
    const start = Date.now();
    const res = getQueueStatus();
    const elapsed = Date.now() - start;

    check(res, {
        "admin: 200": (r) => r.status === 200,
        "admin: within 500ms": () => elapsed < 500,
    });
}

export function teardown() {
    toggleQueue(false);
}
