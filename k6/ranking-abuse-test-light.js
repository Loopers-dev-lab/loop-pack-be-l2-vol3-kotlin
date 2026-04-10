import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
    scenarios: {
        normal_users: {
            executor: 'per-vu-iterations',
            vus: 5,
            iterations: 2,
            startTime: '0s',
            exec: 'normalUser',
        },
        bot_single_ip: {
            executor: 'per-vu-iterations',
            vus: 1,
            iterations: 10,
            startTime: '0s',
            exec: 'botSingleIp',
        },
        bot_no_ua: {
            executor: 'per-vu-iterations',
            vus: 5,
            iterations: 1,
            startTime: '0s',
            exec: 'botNoUa',
        },
        check_rankings: {
            executor: 'per-vu-iterations',
            vus: 1,
            iterations: 1,
            startTime: '10s',
            exec: 'checkRankings',
        },
    },
};

export function normalUser() {
    const vuId = __VU;
    const params = {
        headers: {
            'X-Loopers-LoginId': `user${vuId}`,
            'User-Agent': 'Mozilla/5.0 (Macintosh)',
            'Referer': 'https://example.com',
        },
    };
    const res = http.get(`${BASE_URL}/api/v1/products/1`, params);
    check(res, { 'normal 200': (r) => r.status === 200 });
    sleep(1);
}

export function botSingleIp() {
    const res = http.get(`${BASE_URL}/api/v1/products/2`);
    check(res, { 'bot 200': (r) => r.status === 200 });
    sleep(0.05);
}

export function botNoUa() {
    const params = { headers: {} };
    const res = http.get(`${BASE_URL}/api/v1/products/3`, params);
    check(res, { 'bot_noua 200': (r) => r.status === 200 });
}

export function checkRankings() {
    sleep(5);
    const res = http.get(`${BASE_URL}/api/v1/rankings?size=10`);
    if (res.status === 200) {
        const body = JSON.parse(res.body);
        const rankings = body.data?.rankings || [];
        console.log('========== 랭킹 결과 ==========');
        rankings.forEach((r) => {
            let label = '';
            if (r.productId == 1) label = ' <-- 정상 유저';
            if (r.productId == 2) label = ' <-- 단일IP 봇';
            if (r.productId == 3) label = ' <-- 비로그인+UA없음';
            console.log(`  ${r.rank}위: 상품${r.productId} (score: ${r.score.toFixed(4)})${label}`);
        });
        if (rankings.length === 0) console.log('  (랭킹 데이터 없음 - Consumer 처리 대기 필요)');
        console.log('================================');
    }
}
