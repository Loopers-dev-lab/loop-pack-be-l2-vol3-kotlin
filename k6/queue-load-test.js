import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.1.0/index.js';

// ============================================================
// 설계 기준
// - 블랙프라이데이 가정: 초당 1,000명 대기열 진입
// - 시스템 처리 목표: 175 TPS
// - 스케줄러: 100ms마다 18명 토큰 발급
// - 대기열이 시스템을 보호하는지 검증
// ============================================================

// ============================================================
// 커스텀 메트릭
// ============================================================
const queueEnterDuration = new Trend('queue_enter_duration', true);
const queuePositionDuration = new Trend('queue_position_duration', true);
const orderDuration = new Trend('order_duration', true);
const orderSuccessRate = new Rate('order_success_rate');
const orderSuccessCount = new Counter('order_success_count');
const orderFailCount = new Counter('order_fail_count');
const tokenWaitDuration = new Trend('token_wait_duration', true);

// ============================================================
// 설정
// ============================================================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TEST_PASSWORD = 'Password1!';
const ADMIN_LDAP = 'loopers.admin';
const USER_COUNT = 1000;

// ============================================================
// 시나리오 정의
// ============================================================
export const options = {
    scenarios: {
        // 시나리오 1: 블랙프라이데이 — 대기열 진입 폭증
        // 500명이 30초간 동시에 대기열 진입 시도
        blackfriday_enter: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '5s', target: 1000 },   // 5초에 걸쳐 1000명까지 증가
                { duration: '30s', target: 1000 },   // 30초간 유지
                { duration: '5s', target: 0 },       // 5초에 걸쳐 감소
            ],
            exec: 'blackfridayEnterScenario',
            tags: { scenario: 'blackfriday_enter' },
        },

        // 시나리오 2: 전체 흐름 — 진입 → Polling → 토큰 → 주문
        // 실제 유저 경험 시뮬레이션
        full_flow: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '5s', target: 100 },
                { duration: '50s', target: 100 },
                { duration: '5s', target: 0 },
            ],
            exec: 'fullFlowScenario',
            startTime: '45s',
            tags: { scenario: 'full_flow' },
        },

        // 시나리오 3: 스파이크 — 0명에서 갑자기 500명 진입
        spike: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1s', target: 1000 },   // 1초 만에 1000명 폭증
                { duration: '10s', target: 1000 },
                { duration: '5s', target: 0 },
            ],
            exec: 'blackfridayEnterScenario',
            startTime: '110s',
            tags: { scenario: 'spike' },
        },
    },

    setupTimeout: '180s',

    thresholds: {
        'queue_enter_duration': ['p(95)<500'],
        'queue_position_duration': ['p(95)<200'],
        'order_duration': ['p(95)<2000'],
    },
};

// ============================================================
// 셋업: 테스트 유저 + 브랜드 + 상품 생성
// ============================================================
export function setup() {
    const adminHeaders = {
        'Content-Type': 'application/json',
        'X-Loopers-Ldap': ADMIN_LDAP,
    };

    // 테스트 유저 생성
    const users = [];
    for (let i = 1; i <= USER_COUNT; i++) {
        const loginId = `k6user${i}`;
        http.post(
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
        { headers: adminHeaders }
    );
    const brandBody = JSON.parse(brandRes.body);
    if (!brandBody.data || !brandBody.data.id) {
        console.error(`브랜드 생성 실패: ${brandRes.body}`);
        return { users, productId: 0 };
    }
    const brandId = brandBody.data.id;

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
        { headers: adminHeaders }
    );
    const productBody = JSON.parse(productRes.body);
    if (!productBody.data || !productBody.data.id) {
        console.error(`상품 생성 실패: ${productRes.body}`);
        return { users, productId: 0 };
    }

    console.log(`Setup 완료: 유저 ${users.length}명, 상품 ID ${productBody.data.id}`);
    return { users, productId: productBody.data.id };
}

// ============================================================
// 시나리오 1: 블랙프라이데이 — 대기열 진입 폭증
// - 대기열이 대량 진입을 감당하는지 확인
// - Redis Sorted Set 성능 검증
// ============================================================
export function blackfridayEnterScenario(data) {
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

    // Polling도 함께 (실제 유저 행동)
    const posRes = http.get(`${BASE_URL}/api/v1/queue/position`, {
        headers: {
            'X-Loopers-LoginId': user.loginId,
            'X-Loopers-LoginPw': user.password,
        },
    });

    queuePositionDuration.add(posRes.timings.duration);
    check(posRes, {
        'queue position: status 200': (r) => r.status === 200,
    });

    sleep(2); // 2초마다 Polling
}

// ============================================================
// 시나리오 2: 전체 흐름 — 진입 → Polling → 토큰 → 주문
// - 실제 유저 경험 시뮬레이션
// - 토큰 발급 대기 시간 측정
// - Thundering Herd 관찰 (배치 18명 동시 주문)
// ============================================================
export function fullFlowScenario(data) {
    const userIndex = (__VU - 1) % data.users.length;
    const user = data.users[userIndex];
    const headers = {
        'X-Loopers-LoginId': user.loginId,
        'X-Loopers-LoginPw': user.password,
        'Content-Type': 'application/json',
    };

    // 1. 대기열 진입
    const enterRes = http.post(`${BASE_URL}/api/v1/queue/enter`, null, { headers });
    queueEnterDuration.add(enterRes.timings.duration);

    if (enterRes.status !== 200) {
        return;
    }

    // 2. Polling으로 토큰 대기
    const tokenWaitStart = Date.now();
    let token = null;

    for (let i = 0; i < 20; i++) {
        const posRes = http.get(`${BASE_URL}/api/v1/queue/position`, { headers });
        queuePositionDuration.add(posRes.timings.duration);

        if (posRes.status === 200) {
            const body = JSON.parse(posRes.body);
            if (body.data && body.data.token) {
                token = body.data.token;
                tokenWaitDuration.add(Date.now() - tokenWaitStart);
                break;
            }
        }
        sleep(2); // 2초마다 Polling
    }

    // 3. 토큰이 있으면 주문 (Thundering Herd 발생 지점)
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
            orderSuccessCount.add(1);
        } else {
            orderFailCount.add(1);
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
