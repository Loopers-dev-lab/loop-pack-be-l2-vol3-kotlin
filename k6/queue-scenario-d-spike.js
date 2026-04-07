import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

/**
 * 시나리오 D: 통합 스파이크 테스트
 *
 * 목적: 실제 블프 시나리오 시뮬레이션
 * 흐름: 대기열 진입 → polling → 토큰 수신 → 주문 API 호출
 * 측정: 전체 흐름 완료율, 주문 성공률, DB 커넥션 안정성
 *
 * 사전 조건:
 *   1. queue.enabled=true
 *   2. 테스트용 유저/상품 데이터 사전 등록
 *
 * 실행:
 *   k6 run k6/queue-scenario-d-spike.js
 *   k6 run --env VUS=10000 k6/queue-scenario-d-spike.js
 */

const enterLatency = new Trend('enter_latency', true);
const pollingLatency = new Trend('polling_latency', true);
const orderLatency = new Trend('order_latency', true);
const orderErrorRate = new Rate('order_errors');
const flowComplete = new Counter('flow_complete');
const orderSuccess = new Counter('order_success');

const VUS = parseInt(__ENV.VUS || '10000');
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
    scenarios: {
        spike: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: VUS },   // 10초 안에 스파이크
                { duration: '120s', target: VUS },   // 2분간 유지 (대기열 소진 대기)
                { duration: '10s', target: 0 },
            ],
        },
    },
    thresholds: {
        order_errors: ['rate<0.05'],
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

    // Step 1: 대기열 진입
    const enterRes = http.post(`${BASE_URL}/api/v1/queue/enter`, null, { headers });
    enterLatency.add(enterRes.timings.duration);

    if (enterRes.status !== 200) {
        return;
    }

    const enterData = JSON.parse(enterRes.body).data;
    if (enterData.alreadyHasToken) {
        // 이미 토큰이 있으면 바로 주문
        placeOrder(headers);
        return;
    }

    // Step 2: Polling으로 토큰 대기
    let ready = false;
    let maxPolls = 300; // 최대 300회 polling (5분)
    while (!ready && maxPolls > 0) {
        const pollRes = http.get(`${BASE_URL}/api/v1/queue/position`, { headers });
        pollingLatency.add(pollRes.timings.duration);

        if (pollRes.status === 200) {
            const pollData = JSON.parse(pollRes.body).data;
            if (pollData.status === 'READY') {
                ready = true;
                break;
            }
            // 서버가 제안한 polling 주기 사용
            const pollInterval = pollData.suggestedPollIntervalMs || 1000;
            sleep(pollInterval / 1000);
        } else {
            sleep(1);
        }
        maxPolls--;
    }

    if (!ready) {
        return;
    }

    // Step 3: 주문
    placeOrder(headers);
}

function placeOrder(headers) {
    const payload = JSON.stringify({
        items: [
            { productId: 1, quantity: 1 },
        ],
    });

    const res = http.post(`${BASE_URL}/api/v1/orders`, payload, { headers });
    orderLatency.add(res.timings.duration);

    const success = check(res, {
        'order status 201': (r) => r.status === 201,
    });

    if (success) {
        orderSuccess.add(1);
        flowComplete.add(1);
    } else {
        orderErrorRate.add(true);
    }
}
