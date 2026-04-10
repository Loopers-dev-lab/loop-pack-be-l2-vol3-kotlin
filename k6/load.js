import { check, sleep } from "k6";
import { Counter, Trend } from "k6/metrics";
import {
    registerUser, toggleQueue, enterQueue, getPosition, getQueueStatus,
    pollUntilToken, createOrder, createOrderWithoutToken, parseBody,
} from "./helpers.js";

// --- Custom Metrics ---
const tokenConversions = new Counter("token_conversions");
const tokenAttempts = new Counter("token_attempts");
const retryAfterDeviation = new Trend("retry_after_deviation_ms");

/**
 * Load Test — 혼합 워크로드 정상 트래픽 검증
 *
 * ============================================================
 * VU 산출 근거 (역산)
 * ============================================================
 *
 * 시스템 제약:
 *   DB Pool=50, batchSize=300/3s=100명/s, Token TTL=300s
 *
 * 트래픽 비율 (커머스 대기열 패턴):
 *   진입 30% : 폴링 50% : 주문 15% : 비정상 3% : 모니터 2%
 *
 *   시나리오       Peak VU   근거
 *   ─────────────────────────────────────────────────────────
 *   entrants       300      스케줄러 100명/s × 3s = 300 (1배치 분량)
 *   pollers        500      300대기 / retryAfter 2s = 150 poll/s → VU=500 (마진 3x)
 *   orderers       150      100명/s 입장 × 1.5x 마진
 *   invalidAttempts 30      전체의 3% (악의적/실수 요청)
 *   monitors         2      5초 간격 상태 확인
 *
 * SLO: p95 ≤ 2s (enter), p99 ≤ 1s (position), p95 ≤ 3s (order)
 */
export const options = {
    scenarios: {
        entrants: {
            executor: "ramping-vus",
            stages: [
                { duration: "30s", target: 60 },
                { duration: "1m", target: 150 },
                { duration: "2m", target: 300 },
                { duration: "1m", target: 150 },
                { duration: "30s", target: 0 },
            ],
            exec: "entrant",
        },
        pollers: {
            executor: "ramping-vus",
            stages: [
                { duration: "30s", target: 100 },
                { duration: "1m", target: 250 },
                { duration: "2m", target: 500 },
                { duration: "1m", target: 250 },
                { duration: "30s", target: 0 },
            ],
            exec: "poller",
        },
        orderers: {
            executor: "ramping-vus",
            stages: [
                { duration: "30s", target: 30 },
                { duration: "1m", target: 75 },
                { duration: "2m", target: 150 },
                { duration: "1m", target: 75 },
                { duration: "30s", target: 0 },
            ],
            exec: "orderer",
        },
        invalidAttempts: {
            executor: "ramping-vus",
            stages: [
                { duration: "30s", target: 5 },
                { duration: "1m", target: 15 },
                { duration: "2m", target: 30 },
                { duration: "1m", target: 15 },
                { duration: "30s", target: 0 },
            ],
            exec: "invalidAttempt",
        },
        monitors: {
            executor: "constant-vus",
            vus: 2,
            duration: "5m",
            exec: "monitor",
        },
    },
    thresholds: {
        "http_req_failed{name:enter}": ["rate<0.05"],
        "http_req_failed{name:position}": ["rate<0.05"],
        "http_req_duration{name:enter}": ["p(95)<2000", "p(99)<3000"],
        "http_req_duration{name:position}": ["p(95)<500", "p(99)<1000"],
        "http_req_duration{name:order}": ["p(95)<3000"],
        "http_req_duration{name:adminStatus}": ["p(95)<15000"],
    },
};

export function setup() {
    toggleQueue(true);
    return { ready: true };
}

// --- 진입자: 등록 + 대기열 진입 + 순번 확인 ---
export function entrant() {
    const loginId = `loadenter${__VU}`;
    if (__ITER === 0) registerUser(loginId);

    const res = enterQueue(loginId);
    check(res, { "enter ok": (r) => r.status === 200 });

    const body = parseBody(res);
    const retryAfter = (body && body.data) ? body.data.retryAfter : 3;

    check(body, {
        "position >= 0": (b) => b && b.data && b.data.position >= 0,
        "retryAfter >= 0": (b) => b && b.data && b.data.retryAfter >= 0,
    });

    sleep(retryAfter);
}

// --- 폴러: 반복 순번 조회 (retryAfter 준수 + 편차 측정) ---
export function poller() {
    const loginId = `loadpoll${__VU}`;
    if (__ITER === 0) {
        registerUser(loginId);
        enterQueue(loginId);
    }

    const before = Date.now();
    const res = getPosition(loginId);
    check(res, { "position ok": (r) => r.status === 200 });

    const body = parseBody(res);
    if (body && body.data && body.data.token) {
        check(body, { "token received": (b) => b.data.token.length > 0 });
    }

    const retryAfter = (body && body.data) ? body.data.retryAfter : 3;

    // retryAfter 준수: 서버 권장 간격 대비 실제 Polling 간격 편차 기록
    if (__ITER > 0) {
        const elapsed = Date.now() - before;
        retryAfterDeviation.add(Math.abs(elapsed - retryAfter * 1000));
    }

    sleep(retryAfter);
}

// --- 주문자: 토큰 획득 후 주문 (전환율 추적) ---
export function orderer() {
    const loginId = `loadorder${__VU}`;
    if (__ITER === 0) registerUser(loginId);

    enterQueue(loginId);
    sleep(1);

    tokenAttempts.add(1);
    const token = pollUntilToken(loginId, 15);
    if (token) {
        tokenConversions.add(1);
        const orderRes = createOrder(loginId, token);
        check(orderRes, {
            "order accepted": (r) => r.status === 202 || r.status === 200,
        });
    } else {
        check(null, { "token timeout (expected under load)": () => true });
    }

    sleep(2);
}

// --- 비정상 주문 시도: 토큰 없이/위조 토큰 ---
export function invalidAttempt() {
    const loginId = `loadinvalid${__VU}`;
    if (__ITER === 0) registerUser(loginId);

    if (__ITER % 2 === 0) {
        // 토큰 없이 주문
        const res = createOrderWithoutToken(loginId);
        check(res, { "no token: 403": (r) => r.status === 403 });
    } else {
        // 위조 토큰으로 주문
        const res = createOrder(loginId, "forged-token-" + __VU);
        check(res, { "forged token: 403": (r) => r.status === 403 });
    }

    sleep(3);
}

// --- 어드민 모니터: 상태 주기 조회 ---
export function monitor() {
    const res = getQueueStatus();
    const body = parseBody(res);
    check(res, { "admin status ok": (r) => r.status === 200 });
    check(body, {
        "totalWaiting valid": (b) => b && b.data && b.data.totalWaiting >= 0,
        "activeTokens valid": (b) => b && b.data && b.data.activeTokens >= 0,
    });
    sleep(5);
}

export function teardown() {
    toggleQueue(false);
}
