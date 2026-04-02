import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.1.0/index.js';

// ============================================================
// 커스텀 메트릭
// ============================================================
const queueEnterDuration = new Trend('queue_enter_duration', true);
const queuePositionDuration = new Trend('queue_position_duration', true);
const orderDuration = new Trend('order_duration', true);
const orderSuccessRate = new Rate('order_success_rate');
const orderTPS = new Counter('order_total_success');

// ============================================================
// 설정
// ============================================================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TEST_PASSWORD = 'Password1!';
const ADMIN_LDAP = 'loopers.admin';

// ============================================================
// 시나리오 정의
// ============================================================
export const options = {
    scenarios: {
        // 시나리오 1: 대기열 진입 부하
        queue_enter: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 100 },
                { duration: '30s', target: 100 },
                { duration: '10s', target: 0 },
            ],
            exec: 'queueEnterScenario',
            tags: { scenario: 'queue_enter' },
        },

        // 시나리오 2: Polling 부하
        queue_polling: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 200 },
                { duration: '30s', target: 200 },
                { duration: '10s', target: 0 },
            ],
            exec: 'queuePollingScenario',
            startTime: '55s',
            tags: { scenario: 'queue_polling' },
        },

        // 시나리오 3: 주문 처리량 측정
        order_throughput: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 50 },
                { duration: '30s', target: 50 },
                { duration: '10s', target: 0 },
            ],
            exec: 'orderThroughputScenario',
            startTime: '110s',
            tags: { scenario: 'order_throughput' },
        },
    },

    thresholds: {
        'queue_enter_duration': ['p(99)<500'],
        'queue_position_duration': ['p(99)<100'],
        'order_success_rate': ['rate>0.9'],
    },
};

// ============================================================
// 셋업: 테스트 유저 + 브랜드 + 상품 생성
// ============================================================
export function setup() {
    // 테스트 유저 100명 생성
    const users = [];
    for (let i = 1; i <= 100; i++) {
        const loginId = `k6user${i}`;
        const signUpRes = http.post(
            `${BASE_URL}/api/v1/users`,
            JSON.stringify({
                loginId: loginId,
                password: TEST_PASSWORD,
                name: `k6테스트유저${i}`,
                birthDate: '1990-01-15',
                email: `k6user${i}@test.com`,
            }),
            { headers: { 'Content-Type': 'application/json' } }
        );
        users.push({ loginId, password: TEST_PASSWORD });
    }

    // 브랜드 생성
    const brandRes = http.post(
        `${BASE_URL}/api-admin/v1/brands`,
        JSON.stringify({ name: 'k6테스트브랜드', description: 'k6 부하 테스트용' }),
        {
            headers: {
                'Content-Type': 'application/json',
                'X-Loopers-Ldap': ADMIN_LDAP,
            },
        }
    );
    const brandId = JSON.parse(brandRes.body).data.id;

    // 상품 생성 (재고 충분히)
    const productRes = http.post(
        `${BASE_URL}/api-admin/v1/products`,
        JSON.stringify({
            brandId: brandId,
            name: 'k6테스트상품',
            price: 10000,
            stock: 999999,
            description: 'k6 부하 테스트용 상품',
        }),
        {
            headers: {
                'Content-Type': 'application/json',
                'X-Loopers-Ldap': ADMIN_LDAP,
            },
        }
    );
    const productId = JSON.parse(productRes.body).data.id;

    return { users, productId };
}

// ============================================================
// 시나리오 1: 대기열 진입 부하
// ============================================================
export function queueEnterScenario(data) {
    const userIndex = (__VU - 1) % data.users.length;
    const user = data.users[userIndex];

    const res = http.post(`${BASE_URL}/api/v1/queue/enter`, null, {
        headers: {
            'X-Loopers-LoginId': user.loginId,
            'X-Loopers-LoginPw': user.password,
            'Content-Type': 'application/json',
        },
    });

    queueEnterDuration.add(res.timings.duration);
    check(res, {
        'queue enter: status 200': (r) => r.status === 200,
    });

    sleep(1);
}

// ============================================================
// 시나리오 2: Polling 부하
// ============================================================
export function queuePollingScenario(data) {
    const userIndex = (__VU - 1) % data.users.length;
    const user = data.users[userIndex];

    const res = http.get(`${BASE_URL}/api/v1/queue/position`, {
        headers: {
            'X-Loopers-LoginId': user.loginId,
            'X-Loopers-LoginPw': user.password,
        },
    });

    queuePositionDuration.add(res.timings.duration);
    check(res, {
        'queue position: status 200': (r) => r.status === 200,
    });

    sleep(2);
}

// ============================================================
// 시나리오 3: 주문 처리량 측정
// - 토큰이 있는 유저만 주문 시도
// ============================================================
export function orderThroughputScenario(data) {
    const userIndex = (__VU - 1) % data.users.length;
    const user = data.users[userIndex];
    const headers = {
        'X-Loopers-LoginId': user.loginId,
        'X-Loopers-LoginPw': user.password,
        'Content-Type': 'application/json',
    };

    // 1. 대기열 진입
    http.post(`${BASE_URL}/api/v1/queue/enter`, null, { headers });

    // 2. 토큰 확인 (Polling)
    let token = null;
    for (let i = 0; i < 30; i++) {
        const posRes = http.get(`${BASE_URL}/api/v1/queue/position`, { headers });
        if (posRes.status === 200) {
            const body = JSON.parse(posRes.body);
            if (body.data && body.data.token) {
                token = body.data.token;
                break;
            }
        }
        sleep(1);
    }

    // 3. 토큰이 있으면 주문
    if (token) {
        const orderRes = http.post(
            `${BASE_URL}/api/v1/orders`,
            JSON.stringify({
                items: [{ productId: data.productId, quantity: 1 }],
            }),
            { headers }
        );

        orderDuration.add(orderRes.timings.duration);
        const success = orderRes.status === 200;
        orderSuccessRate.add(success);
        if (success) {
            orderTPS.add(1);
        }

        check(orderRes, {
            'order: status 200': (r) => r.status === 200,
        });
    }
}

// ============================================================
// 결과 요약
// ============================================================
export function handleSummary(data) {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    return {
        [`k6/results/queue-load-test-${timestamp}.json`]: JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}
