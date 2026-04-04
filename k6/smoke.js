import http from "k6/http";
import { check, sleep, group } from "k6";
import {
    BASE_URL, PASSWORD, memberHeaders, registerTestUsers, registerUser,
    toggleQueue, enterQueue, getPosition, getQueueStatus,
    pollUntilToken, createOrder, createOrderWithoutToken, parseBody,
} from "./helpers.js";

/**
 * Smoke Test — 시나리오별 기능 정상 동작 확인 (11개 시나리오)
 *
 *  1. E2E 풀플로우: 진입 → Polling → 토큰 발급 → 주문
 *  2. 멱등 진입: 동일 유저 중복 진입 시 동일 순번
 *  3. 토큰 없이 주문: 403 거부 확인
 *  4. 비활성 대기열: 진입 시 에러 확인
 *  5. 어드민 상태 조회: totalWaiting/activeTokens 정합성
 *  6. 위조 토큰으로 주문: 403 거부
 *  7. 주문 완료 후 재진입: 토큰 소멸 → 재대기 가능
 *  8. 순번 감소 검증: 스케줄러 동작 시 position 감소
 *  9. 동시 중복 진입: 같은 유저 병렬 진입 → 멱등
 * 10. 트래픽 중 토글 OFF: 기존 대기자 순번 조회 + 신규 진입 거부
 * 11. retryAfter 동적 조정: position 감소 시 retryAfter도 감소
 */
export const options = {
    scenarios: {
        e2eFlow: {
            executor: "shared-iterations",
            vus: 5,
            iterations: 5,
            exec: "e2eFlow",
        },
        idempotentEntry: {
            executor: "shared-iterations",
            vus: 3,
            iterations: 3,
            exec: "idempotentEntry",
            startTime: "0s",
        },
        orderWithoutToken: {
            executor: "shared-iterations",
            vus: 2,
            iterations: 2,
            exec: "orderWithoutToken",
            startTime: "0s",
        },
        invalidToken: {
            executor: "shared-iterations",
            vus: 2,
            iterations: 2,
            exec: "invalidToken",
            startTime: "0s",
        },
        reEntryAfterOrder: {
            executor: "shared-iterations",
            vus: 2,
            iterations: 2,
            exec: "reEntryAfterOrder",
            startTime: "0s",
        },
        positionDecrement: {
            executor: "shared-iterations",
            vus: 1,
            iterations: 1,
            exec: "positionDecrement",
            startTime: "0s",
        },
        concurrentEntry: {
            executor: "shared-iterations",
            vus: 1,
            iterations: 1,
            exec: "concurrentEntry",
            startTime: "0s",
        },
        retryAfterDynamic: {
            executor: "shared-iterations",
            vus: 1,
            iterations: 1,
            exec: "retryAfterDynamic",
            startTime: "0s",
        },
        adminStatus: {
            executor: "shared-iterations",
            vus: 1,
            iterations: 3,
            exec: "adminStatus",
            startTime: "5s",
        },
        toggleMidTraffic: {
            executor: "shared-iterations",
            vus: 1,
            iterations: 1,
            exec: "toggleMidTraffic",
            startTime: "35s",
        },
        disabledQueue: {
            executor: "shared-iterations",
            vus: 1,
            iterations: 1,
            exec: "disabledQueue",
            startTime: "45s",
        },
    },
    thresholds: {
        "http_req_failed{name:enter}": ["rate<0.05"],
        // position threshold 미적용: 엣지 케이스 시나리오에서 404 등 예상 응답 포함
        "http_req_duration{name:enter}": ["p(95)<1000"],
        "http_req_duration{name:position}": ["p(95)<500"],
        "checks": ["rate>0.90"],
    },
};

export function setup() {
    registerTestUsers("smokeuser", 30);
    toggleQueue(true);
    return { ready: true };
}

// --- Scenario 1: E2E 풀플로우 ---
export function e2eFlow() {
    const loginId = `smokeuser${__VU}`;

    group("1-enter", () => {
        const res = enterQueue(loginId);
        check(res, {
            "enter 200": (r) => r.status === 200,
            "has position": (r) => {
                const body = parseBody(r);
                return body && body.data && body.data.position >= 0;
            },
            "has retryAfter": (r) => {
                const body = parseBody(r);
                return body && body.data && body.data.retryAfter >= 0;
            },
            "has estimatedWait": (r) => {
                const body = parseBody(r);
                return body && body.data && body.data.estimatedWaitSeconds >= 0;
            },
        });
    });

    group("2-poll", () => {
        const token = pollUntilToken(loginId, 30);
        check(token, {
            "token issued": (t) => t !== null && t.length > 0,
        });

        if (token) {
            group("3-order", () => {
                const orderRes = createOrder(loginId, token);
                check(orderRes, {
                    "order accepted": (r) => r.status === 202 || r.status === 200,
                });
            });
        }
    });
}

// --- Scenario 2: 멱등 진입 ---
export function idempotentEntry() {
    const loginId = `smokeuser${__VU + 5}`;

    const first = enterQueue(loginId);
    const firstBody = parseBody(first);
    const second = enterQueue(loginId);
    const secondBody = parseBody(second);

    check(null, {
        "idempotent: both 200": () => first.status === 200 && second.status === 200,
        "idempotent: same position": () => {
            if (!firstBody || !firstBody.data || !secondBody || !secondBody.data) return false;
            return firstBody.data.position === secondBody.data.position;
        },
    });
}

// --- Scenario 3: 대기열 활성 상태에서 토큰 없이 주문 → 403 ---
export function orderWithoutToken() {
    const loginId = `smokeuser${__VU + 8}`;

    const res = createOrderWithoutToken(loginId);
    check(res, {
        "order without token: 403": (r) => r.status === 403,
    });
}

// --- Scenario 4: 위조 토큰으로 주문 → 403 ---
export function invalidToken() {
    const loginId = `smokeuser${__VU + 10}`;

    const fakeToken = "fake-token-00000000-0000-0000-0000-000000000000";
    const res = createOrder(loginId, fakeToken);
    check(res, {
        "invalid token: 403": (r) => r.status === 403,
    });
}

// --- Scenario 5: 주문 완료 후 재진입 ---
export function reEntryAfterOrder() {
    const loginId = `smokeuser${__VU + 12}`;

    // 1. 진입 → 토큰 획득 → 주문
    enterQueue(loginId);
    const token = pollUntilToken(loginId, 30);

    if (token) {
        createOrder(loginId, token);
        sleep(1);

        // 2. 토큰 소멸 후 재진입 가능 여부
        const reEnterRes = enterQueue(loginId);
        const body = parseBody(reEnterRes);
        check(reEnterRes, {
            "re-entry: 200": (r) => r.status === 200,
        });
        check(body, {
            "re-entry: position assigned": (b) => b && b.data && b.data.position >= 0,
        });
    }
}

// --- Scenario 6: 순번 감소 검증 (스케줄러 3초 주기) ---
export function positionDecrement() {
    // 여러 유저를 대기열에 넣고, 스케줄러 처리 후 position 감소 확인
    const watchers = [];
    for (let i = 16; i <= 20; i++) {
        enterQueue(`smokeuser${i}`);
        watchers.push(`smokeuser${i}`);
    }

    // 첫 번째 유저의 초기 순번
    const initialRes = getPosition(watchers[watchers.length - 1]);
    const initialBody = parseBody(initialRes);
    const initialPosition = (initialBody && initialBody.data) ? initialBody.data.position : -1;

    // 스케줄러 주기 대기 (3초 × 2 = 6초)
    sleep(7);

    // 순번 재조회
    const afterRes = getPosition(watchers[watchers.length - 1]);
    const afterBody = parseBody(afterRes);
    const afterPosition = (afterBody && afterBody.data) ? afterBody.data.position : -1;

    check(null, {
        "position decreased or token issued": () => {
            // 토큰 발급됨 (position 0) 또는 순번 감소
            if (afterBody && afterBody.data && afterBody.data.token) return true;
            return afterPosition < initialPosition || afterPosition === 0;
        },
    });
}

// --- Scenario 7: 동시 중복 진입 (batch로 동일 유저 요청) ---
export function concurrentEntry() {
    const loginId = "smokeuser21";

    // k6는 진정한 병렬은 아니지만, 빠르게 연속 호출로 경쟁 조건 시뮬레이션
    const responses = http.batch([
        ["POST", `${BASE_URL}/api/v1/queue/enter`, null, { headers: memberHeaders(loginId), tags: { name: "enter" } }],
        ["POST", `${BASE_URL}/api/v1/queue/enter`, null, { headers: memberHeaders(loginId), tags: { name: "enter" } }],
        ["POST", `${BASE_URL}/api/v1/queue/enter`, null, { headers: memberHeaders(loginId), tags: { name: "enter" } }],
    ]);

    const positions = responses
        .map((r) => parseBody(r))
        .filter((b) => b && b.data)
        .map((b) => b.data.position);

    check(null, {
        "concurrent: all same position": () => {
            if (positions.length === 0) return false;
            return positions.every((p) => p === positions[0]);
        },
        "concurrent: all 200": () => responses.every((r) => r.status === 200),
    });
}

// --- Scenario 8: retryAfter 동적 조정 ---
export function retryAfterDynamic() {
    const loginId = "smokeuser22";

    enterQueue(loginId);
    const firstRes = getPosition(loginId);
    const firstBody = parseBody(firstRes);
    const firstRetry = (firstBody && firstBody.data) ? firstBody.data.retryAfter : -1;
    const firstPosition = (firstBody && firstBody.data) ? firstBody.data.position : -1;

    // 스케줄러 대기
    sleep(7);

    const secondRes = getPosition(loginId);
    const secondBody = parseBody(secondRes);
    const secondRetry = (secondBody && secondBody.data) ? secondBody.data.retryAfter : -1;
    const secondPosition = (secondBody && secondBody.data) ? secondBody.data.position : -1;

    check(null, {
        "retryAfter: decreased or token": () => {
            // 토큰 발급됨
            if (secondBody && secondBody.data && secondBody.data.token) return true;
            // position 감소 시 retryAfter도 감소 (또는 동일)
            return secondRetry <= firstRetry;
        },
        "retryAfter: consistent with position": () => {
            // position이 줄었으면 retryAfter도 줄어야 함
            if (secondPosition < firstPosition) return secondRetry <= firstRetry;
            return true;
        },
    });
}

// --- Scenario 9: 어드민 상태 조회 ---
export function adminStatus() {
    const res = getQueueStatus();
    const body = parseBody(res);
    check(res, {
        "admin status 200": (r) => r.status === 200,
        "has totalWaiting": () => body && body.data && body.data.totalWaiting >= 0,
        "has activeTokens": () => body && body.data && body.data.activeTokens >= 0,
        "has enabled flag": () => body && body.data && typeof body.data.enabled === "boolean",
    });
    sleep(5);
}

// --- Scenario 10: 트래픽 중 토글 OFF ---
export function toggleMidTraffic() {
    const waiter = "smokeuser23";
    const newcomer = "smokeuser24";

    // 1. 대기자 진입
    enterQueue(waiter);
    sleep(1);

    // 2. 토글 OFF
    toggleQueue(false);
    sleep(0.5);

    // 3. 기존 대기자 순번 조회 — 대기열 데이터는 남아있어야 함
    const posRes = getPosition(waiter);
    check(posRes, {
        "toggle mid: existing waiter can query": (r) => r.status === 200 || r.status === 404,
    });

    // 4. 신규 진입 시도 → 거부
    const newRes = http.post(`${BASE_URL}/api/v1/queue/enter`, null, {
        headers: memberHeaders(newcomer),
        tags: { name: "enterDisabled" },
    });
    check(newRes, {
        "toggle mid: new entry rejected": (r) => r.status !== 200,
    });

    // 5. 복원
    toggleQueue(true);
}

// --- Scenario 11: 비활성 대기열 → 진입 거부 ---
export function disabledQueue() {
    const loginId = `smokeuser${__VU + 25}`;

    toggleQueue(false);
    sleep(0.5);

    const res = http.post(`${BASE_URL}/api/v1/queue/enter`, null, {
        headers: memberHeaders(loginId),
        tags: { name: "enterDisabled" },
    });
    check(res, {
        "disabled queue: rejected": (r) => r.status !== 200,
    });

    toggleQueue(true);
}

export function teardown() {
    toggleQueue(false);
}
