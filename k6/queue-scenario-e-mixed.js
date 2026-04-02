import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

/**
 * 시나리오 E: 극한 혼합 테스트
 *
 * 목적: 대기열 진입 + polling + 주문이 동시에 섞인 상황
 * 설정: 진입 3,000 + polling 5,000 + 주문 500 동시
 * 측정: 각 API별 응답시간, 전체 에러율
 *
 * 사전 조건:
 *   1. queue.enabled=true
 *   2. 테스트용 유저/상품 데이터 사전 등록
 *   3. 주문 시나리오의 유저는 사전에 토큰이 발급된 상태여야 함
 *
 * 실행:
 *   k6 run k6/queue-scenario-e-mixed.js
 */

const enterLatency = new Trend('enter_latency', true);
const pollingLatency = new Trend('polling_latency', true);
const orderLatency = new Trend('order_latency', true);
const enterErrors = new Rate('enter_errors');
const pollingErrors = new Rate('polling_errors');
const orderErrors = new Rate('order_errors');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
    scenarios: {
        enter: {
            executor: 'constant-vus',
            vus: 3000,
            duration: '60s',
            exec: 'enterQueue',
        },
        polling: {
            executor: 'constant-vus',
            vus: 5000,
            duration: '60s',
            exec: 'pollPosition',
        },
        order: {
            executor: 'constant-vus',
            vus: 500,
            duration: '60s',
            exec: 'placeOrder',
        },
    },
    thresholds: {
        enter_latency: ['p(95)<500'],
        polling_latency: ['p(95)<200'],
        order_latency: ['p(95)<3000'],
        enter_errors: ['rate<0.05'],
        polling_errors: ['rate<0.05'],
        order_errors: ['rate<0.1'],
    },
};

function authHeaders(userId) {
    return {
        'Content-Type': 'application/json',
        'X-Loopers-LoginId': `loadtest_user_${userId}`,
        'X-Loopers-LoginPw': 'Test123!',
    };
}

export function enterQueue() {
    const userId = 10000 + __VU;
    const headers = authHeaders(userId);

    const res = http.post(`${BASE_URL}/api/v1/queue/enter`, null, { headers });
    enterLatency.add(res.timings.duration);

    const success = check(res, {
        'enter status 200': (r) => r.status === 200,
    });
    if (!success) enterErrors.add(true);

    sleep(1);
}

export function pollPosition() {
    const userId = 20000 + __VU;
    const headers = authHeaders(userId);

    const res = http.get(`${BASE_URL}/api/v1/queue/position`, { headers });
    pollingLatency.add(res.timings.duration);

    const success = check(res, {
        'poll status 200': (r) => r.status === 200,
    });
    if (!success) pollingErrors.add(true);

    sleep(2);
}

export function placeOrder() {
    const userId = 30000 + __VU;
    const headers = authHeaders(userId);

    const payload = JSON.stringify({
        items: [
            { productId: 1, quantity: 1 },
        ],
    });

    const res = http.post(`${BASE_URL}/api/v1/orders`, payload, { headers });
    orderLatency.add(res.timings.duration);

    const success = check(res, {
        'order status 201 or 403': (r) => r.status === 201 || r.status === 403,
    });
    if (!success) orderErrors.add(true);

    sleep(1);
}
