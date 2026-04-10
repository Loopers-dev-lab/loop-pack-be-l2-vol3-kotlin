import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Counter } from 'k6/metrics';

/**
 * 랭킹 조회수 어뷰징 방지 테스트
 *
 * 시나리오:
 *   1. normal_users  — 정상 로그인 유저 100명이 다양한 상품 조회
 *   2. bot_single_ip — 봇 1대가 같은 IP로 같은 상품 반복 조회
 *   3. bot_multi_ip  — 봇이 IP를 바꿔가며 같은 상품 반복 조회 (비로그인)
 *   4. mixed         — 정상 유저 + 봇 혼합 트래픽
 *
 * 검증:
 *   테스트 완료 후 GET /api/v1/rankings 로 각 상품의 점수를 비교한다.
 *   - 상품1 (정상 유저 타겟)   → 높은 점수
 *   - 상품2 (단일 IP 봇 타겟)  → 매우 낮은 점수 (Layer 1에서 차단)
 *   - 상품3 (다중 IP 봇 타겟)  → 낮은 점수 (Layer 2 Trust Score로 감쇠)
 */

const viewLatency = new Trend('view_latency');
const viewCount = new Counter('view_requests');
const rankingLatency = new Trend('ranking_latency');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 테스트 대상 상품 ID (사전에 존재해야 함)
const NORMAL_PRODUCT_ID = __ENV.NORMAL_PRODUCT_ID || 1;   // 정상 유저가 조회할 상품
const BOT_SINGLE_PRODUCT_ID = __ENV.BOT_SINGLE_PRODUCT_ID || 2;  // 단일 IP 봇 타겟
const BOT_MULTI_PRODUCT_ID = __ENV.BOT_MULTI_PRODUCT_ID || 3;    // 다중 IP 봇 타겟

export const options = {
    scenarios: {
        // 시나리오 1: 정상 로그인 유저 100명
        normal_users: {
            executor: 'per-vu-iterations',
            vus: 100,
            iterations: 3,           // 유저당 3회 조회 (다양한 상품)
            startTime: '0s',
            exec: 'normalUser',
        },

        // 시나리오 2: 봇 - 같은 IP로 같은 상품 반복
        bot_single_ip: {
            executor: 'per-vu-iterations',
            vus: 1,
            iterations: 100,         // 1대가 100번 반복
            startTime: '0s',
            exec: 'botSingleIp',
        },

        // 시나리오 3: 봇 - IP 바꿔가며 같은 상품 (비로그인)
        bot_multi_ip: {
            executor: 'per-vu-iterations',
            vus: 100,
            iterations: 1,           // 100개 IP에서 각 1회
            startTime: '0s',
            exec: 'botMultiIp',
        },

        // 시나리오 4: 혼합 트래픽
        mixed_traffic: {
            executor: 'per-vu-iterations',
            vus: 50,
            iterations: 2,
            startTime: '15s',         // 앞 시나리오 완료 후 시작
            exec: 'mixedTraffic',
        },

        // 결과 확인: 랭킹 API 호출
        check_rankings: {
            executor: 'per-vu-iterations',
            vus: 1,
            iterations: 1,
            startTime: '25s',         // 모든 트래픽 완료 후
            exec: 'checkRankings',
        },
    },
};

// ── 시나리오 1: 정상 로그인 유저 ──────────────────────────
export function normalUser() {
    const vuId = __VU;
    const loginId = `normaluser${vuId}`;
    const products = [NORMAL_PRODUCT_ID, NORMAL_PRODUCT_ID + 10, NORMAL_PRODUCT_ID + 20];
    const productId = products[__ITER % products.length];

    const params = {
        headers: {
            'X-Loopers-LoginId': loginId,
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)',
            'Referer': 'https://example.com/products',
        },
    };

    const res = http.get(`${BASE_URL}/api/v1/products/${productId}`, params);
    viewLatency.add(res.timings.duration);
    viewCount.add(1);

    check(res, { 'normal 200': (r) => r.status === 200 });
    sleep(Math.random() * 2 + 1);  // 1~3초 랜덤 대기 (사람처럼)
}

// ── 시나리오 2: 봇 - 같은 IP, 같은 상품 반복 ───────────────
export function botSingleIp() {
    // 로그인 없이, 같은 상품을 빠르게 반복
    const res = http.get(`${BASE_URL}/api/v1/products/${BOT_SINGLE_PRODUCT_ID}`);
    viewLatency.add(res.timings.duration);
    viewCount.add(1);

    check(res, { 'bot_single 200': (r) => r.status === 200 });
    sleep(0.05);  // 50ms 간격 (봇처럼 빠르게)
}

// ── 시나리오 3: 봇 - IP 분산, 같은 상품 ──────────────────
export function botMultiIp() {
    // 각 VU가 다른 "가상 IP"로 동작 (실제로는 같은 IP지만 비로그인)
    // UA, Referer 없이 요청 (봇 특징)
    const params = {
        headers: {
            // UA, Referer 의도적으로 미설정
        },
    };

    const res = http.get(`${BASE_URL}/api/v1/products/${BOT_MULTI_PRODUCT_ID}`, params);
    viewLatency.add(res.timings.duration);
    viewCount.add(1);

    check(res, { 'bot_multi 200': (r) => r.status === 200 });
    // sleep 없음 (봇처럼 즉시)
}

// ── 시나리오 4: 혼합 트래픽 ──────────────────────────────
export function mixedTraffic() {
    const vuId = __VU;

    if (vuId % 2 === 0) {
        // 정상 유저
        const loginId = `mixeduser${vuId}`;
        const params = {
            headers: {
                'X-Loopers-LoginId': loginId,
                'User-Agent': 'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0)',
                'Referer': 'https://example.com',
            },
        };
        const res = http.get(`${BASE_URL}/api/v1/products/${NORMAL_PRODUCT_ID}`, params);
        check(res, { 'mixed_normal 200': (r) => r.status === 200 });
        sleep(1);
    } else {
        // 봇
        const res = http.get(`${BASE_URL}/api/v1/products/${BOT_MULTI_PRODUCT_ID}`);
        check(res, { 'mixed_bot 200': (r) => r.status === 200 });
        sleep(0.05);
    }
}

// ── 결과 확인: 랭킹 API ──────────────────────────────────
export function checkRankings() {
    sleep(3);  // Consumer 처리 대기

    const res = http.get(`${BASE_URL}/api/v1/rankings?size=50`);
    rankingLatency.add(res.timings.duration);

    const success = check(res, { 'ranking 200': (r) => r.status === 200 });

    if (success) {
        const body = JSON.parse(res.body);
        const rankings = body.data?.rankings || [];

        console.log('========== 랭킹 결과 ==========');
        console.log(`총 ${rankings.length}개 상품`);
        console.log('');

        rankings.forEach((r) => {
            let label = '';
            if (r.productId == NORMAL_PRODUCT_ID) label = ' ← 정상 유저 타겟';
            if (r.productId == BOT_SINGLE_PRODUCT_ID) label = ' ← 단일IP 봇 타겟';
            if (r.productId == BOT_MULTI_PRODUCT_ID) label = ' ← 다중IP 봇 타겟';

            console.log(`  ${r.rank}위: 상품${r.productId} (score: ${r.score.toFixed(4)})${label}`);
        });

        console.log('');
        console.log('========== 기대 결과 ==========');
        console.log('정상 유저 타겟 > 다중IP 봇 타겟 >> 단일IP 봇 타겟');
        console.log('================================');
    }
}
