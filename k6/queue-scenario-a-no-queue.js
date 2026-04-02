import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

/**
 * 시나리오 A: 비교군 — 대기열 없이 주문 API 직접 호출
 *
 * 목적: "대기열이 왜 필요한가"의 정량적 근거
 * 핵심: 어느 시점에서 시스템이 무너지는지 특정
 *
 * 사전 조건:
 *   1. application.yml에서 queue.enabled=false 설정
 *   2. 테스트용 유저/상품 데이터 사전 등록
 *   3. DB 커넥션 풀 모니터링 준비 (Actuator /prometheus)
 *
 * 실행:
 *   k6 run k6/queue-scenario-a-no-queue.js
 *   k6 run --env VUS=2000 k6/queue-scenario-a-no-queue.js
 */

const orderLatency = new Trend('order_latency', true);
const orderErrorRate = new Rate('order_errors');
const orderSuccess = new Counter('order_success');
const orderFail = new Counter('order_fail');

const VUS = parseInt(__ENV.VUS || '1000');
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
    scenarios: {
        ramp_up: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: Math.floor(VUS * 0.2) },
                { duration: '20s', target: Math.floor(VUS * 0.5) },
                { duration: '20s', target: VUS },
                { duration: '10s', target: VUS },
                { duration: '10s', target: 0 },
            ],
        },
    },
    thresholds: {
        order_latency: ['p(50)<500', 'p(95)<2000', 'p(99)<5000'],
        order_errors: ['rate<0.1'],
    },
};

export default function () {
    const userId = __VU;
    const loginId = `loaduser${userId}`;
    const password = 'Test1234!';

    const headers = {
        'Content-Type': 'application/json',
        'X-Loopers-LoginId': loginId,
        'X-Loopers-LoginPw': password,
    };

    const payload = JSON.stringify({
        items: [
            { productId: 1, quantity: 1 },
        ],
    });

    const res = http.post(`${BASE_URL}/api/v1/orders`, payload, { headers });
    orderLatency.add(res.timings.duration);

    const success = check(res, {
        'status 201': (r) => r.status === 201,
    });

    if (success) {
        orderSuccess.add(1);
    } else {
        orderFail.add(1);
        orderErrorRate.add(true);
    }

    sleep(0.1);
}
