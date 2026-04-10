import { check, sleep } from "k6";
import {
    registerUser, toggleQueue, enterQueue, getPosition, getQueueStatus,
    parseBody,
} from "./helpers.js";

/**
 * Spike Test — Thundering Herd 시나리오
 *
 * ============================================================
 * VU 산출 근거 (블랙 프라이데이 오픈 모사)
 * ============================================================
 *
 * 시나리오: 00:00 오픈 공지 → 수천 명 동시 F5
 *
 *   시나리오         Peak VU   근거
 *   ─────────────────────────────────────────────────────────
 *   thunderingHerd   5,000    Tomcat max-connections=10,000의 50%
 *                             3초 내 진입 → Redis ZADD 5K ops/3s = 1,667 OPS
 *   lateArrivals     2,000    오픈 30초 후 추가 유입 (입소문/알림)
 *   adminMonitor         2    급증 중 대기열 상태 추적
 *
 * 검증 포인트:
 *   - ZADD NX 동시성: 5,000 동시 → 순번 정확성
 *   - 스케줄러 안정성: 300명/3s 처리 중 추가 유입
 *   - retryAfter 분산: 5K명 × retryAfter 5~10s → 500~1000 poll/s
 *   - accept-count 200 초과 시 커넥션 거부 발생 여부
 *
 * SLO 완화: p95<15s — 극단 시나리오, 장애 발생 패턴 확인 목적
 */
export const options = {
    scenarios: {
        thunderingHerd: {
            executor: "ramping-vus",
            stages: [
                { duration: "5s", target: 0 },
                { duration: "3s", target: 5000 },    // 3초 만에 5,000 동시 진입
                { duration: "1m", target: 5000 },    // 유지하며 Polling
                { duration: "30s", target: 0 },
            ],
            exec: "spikeUser",
        },
        lateArrivals: {
            executor: "ramping-vus",
            startTime: "30s",
            stages: [
                { duration: "5s", target: 2000 },    // 30초 후 추가 2,000명
                { duration: "1m", target: 2000 },
                { duration: "30s", target: 0 },
            ],
            exec: "lateUser",
        },
        adminMonitor: {
            executor: "constant-vus",
            vus: 2,
            duration: "2m30s",
            exec: "monitor",
        },
    },
    thresholds: {
        "http_req_failed{name:enter}": ["rate<0.20"],
        "http_req_duration{name:enter}": ["p(95)<15000"],
        "http_req_duration{name:position}": ["p(95)<10000"],
    },
};

export function setup() {
    toggleQueue(true);
    return { ready: true };
}

// --- 초기 폭주 유저: 진입 + 즉시 Polling ---
export function spikeUser() {
    const loginId = `spikeuser${__VU}`;
    if (__ITER === 0) registerUser(loginId);

    if (__ITER === 0) {
        // 첫 반복: 진입 + 순번 확인
        const enterRes = enterQueue(loginId);
        const ok = enterRes.status === 200;
        check(enterRes, {
            "spike enter handled": () => ok || enterRes.status === 429 || enterRes.status === 503,
        });

        if (ok) {
            const body = parseBody(enterRes);
            check(body, {
                "position assigned": (b) => b && b.data && b.data.position >= 0,
                "totalWaiting > 0": (b) => b && b.data && b.data.totalWaiting > 0,
            });
        }

        const body = parseBody(enterRes);
        const retryAfter = (body && body.data) ? body.data.retryAfter : 5;
        sleep(retryAfter);
    } else {
        // 이후 반복: Polling
        const posRes = getPosition(loginId);
        check(posRes, {
            "position ok": (r) => r.status === 200 || r.status === 404,
        });

        const body = parseBody(posRes);
        if (body && body.data && body.data.token) {
            check(body, { "token received in spike": () => true });
        }

        const retryAfter = (body && body.data) ? body.data.retryAfter : 5;
        sleep(retryAfter);
    }
}

// --- 후발 진입자: 30초 후 추가 유입 ---
export function lateUser() {
    const loginId = `spikelate${__VU}`;
    if (__ITER === 0) registerUser(loginId);

    if (__ITER === 0) {
        const res = enterQueue(loginId);
        check(res, {
            "late enter handled": (r) => r.status === 200 || r.status === 429 || r.status === 503,
        });
    } else {
        const res = getPosition(loginId);
        check(res, {
            "late position ok": (r) => r.status === 200 || r.status === 404,
        });
    }

    const retryAfter = 3 + Math.random() * 2;
    sleep(retryAfter);
}

// --- 어드민 모니터: 급증 중 상태 추적 ---
export function monitor() {
    const res = getQueueStatus();
    const body = parseBody(res);
    check(res, { "admin status ok": (r) => r.status === 200 });

    if (body && body.data) {
        check(body, {
            "spike: totalWaiting tracked": (b) => b.data.totalWaiting >= 0,
            "spike: activeTokens tracked": (b) => b.data.activeTokens >= 0,
        });
    }

    sleep(3);
}

export function teardown() {
    toggleQueue(false);
}
