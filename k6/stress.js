import { check, sleep } from "k6";
import {
    registerUser, toggleQueue, enterQueue, getPosition, getQueueStatus,
    pollUntilToken, createOrder, parseBody,
} from "./helpers.js";

/**
 * Stress Test — 한계 탐색 (혼합 워크로드)
 *
 * ============================================================
 * VU 산출 근거 (역산 × 10배)
 * ============================================================
 *
 * Load 테스트의 10배 스케일. 목적: 병목 지점과 장애 임계값 탐색
 *
 *   시나리오       Peak VU   근거
 *   ─────────────────────────────────────────────────────────
 *   entrants       3,000    Load 300 × 10x (Redis ZADD 한계 탐색)
 *   pollers        5,000    Load 500 × 10x (ZRANK + 인증 부하)
 *   orderers       1,500    Load 150 × 10x (DB Pool 50 포화 지점)
 *   monitors           3    상태 조회 (Pipeline 적용 전/후 비교)
 *
 * 예상 병목:
 *   1순위: DB Pool (50) → 주문 처리 대기
 *   2순위: BCrypt 인증 → CPU bound (캐시 미스 시)
 *   3순위: Tomcat accept-count (200) → 커넥션 거부
 *
 * SLO 완화: p95<5s (enter), p95<3s (position) — 한계 탐색 목적
 */
export const options = {
    scenarios: {
        entrants: {
            executor: "ramping-vus",
            stages: [
                { duration: "1m", target: 500 },
                { duration: "2m", target: 2000 },
                { duration: "2m", target: 3000 },
                { duration: "2m", target: 3000 },
                { duration: "1m", target: 0 },
            ],
            exec: "entrant",
        },
        pollers: {
            executor: "ramping-vus",
            stages: [
                { duration: "1m", target: 1000 },
                { duration: "2m", target: 4000 },
                { duration: "2m", target: 5000 },
                { duration: "2m", target: 5000 },
                { duration: "1m", target: 0 },
            ],
            exec: "poller",
        },
        orderers: {
            executor: "ramping-vus",
            stages: [
                { duration: "1m", target: 200 },
                { duration: "2m", target: 800 },
                { duration: "2m", target: 1500 },
                { duration: "2m", target: 1500 },
                { duration: "1m", target: 0 },
            ],
            exec: "orderer",
        },
        monitors: {
            executor: "constant-vus",
            vus: 3,
            duration: "8m",
            exec: "monitor",
        },
    },
    thresholds: {
        "http_req_failed{name:enter}": ["rate<0.15"],
        "http_req_failed{name:position}": ["rate<0.15"],
        "http_req_duration{name:enter}": ["p(95)<5000"],
        "http_req_duration{name:position}": ["p(95)<3000"],
        "http_req_duration{name:order}": ["p(95)<10000"],
    },
};

export function setup() {
    toggleQueue(true);
    return { ready: true };
}

// --- 진입자 ---
export function entrant() {
    const loginId = `stressenter${__VU}`;
    if (__ITER === 0) registerUser(loginId);

    const res = enterQueue(loginId);
    check(res, { "enter ok": (r) => r.status === 200 || r.status === 429 });

    const body = parseBody(res);
    const retryAfter = (body && body.data) ? body.data.retryAfter : 5;
    sleep(retryAfter);
}

// --- 폴러 ---
export function poller() {
    const loginId = `stresspoll${__VU}`;
    if (__ITER === 0) {
        registerUser(loginId);
        enterQueue(loginId);
    }

    const res = getPosition(loginId);
    check(res, { "position ok": (r) => r.status === 200 || r.status === 404 });

    const body = parseBody(res);
    const retryAfter = (body && body.data) ? body.data.retryAfter : 5;
    sleep(retryAfter);
}

// --- 주문자 ---
export function orderer() {
    const loginId = `stressorder${__VU}`;
    if (__ITER === 0) registerUser(loginId);

    enterQueue(loginId);
    sleep(1);

    const token = pollUntilToken(loginId, 10);
    if (token) {
        const orderRes = createOrder(loginId, token);
        check(orderRes, {
            "order handled": (r) => r.status === 202 || r.status === 200 || r.status === 429,
        });
    }

    sleep(3);
}

// --- 어드민 모니터 ---
export function monitor() {
    const res = getQueueStatus();
    check(res, { "admin status ok": (r) => r.status === 200 });
    sleep(10);
}

export function teardown() {
    toggleQueue(false);
}
