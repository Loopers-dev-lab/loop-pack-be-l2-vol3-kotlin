import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

/**
 * 데이터 규모별 DB vs Redis 랭킹 조회 성능 비교
 *
 * 실행:
 *   k6 run --env TARGET=redis k6/ranking-scale-comparison.js
 *   k6 run --env TARGET=db k6/ranking-scale-comparison.js
 */

const latency = new Trend('ranking_latency', true);

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TARGET = __ENV.TARGET || 'redis';
const TODAY = new Date().toISOString().slice(0, 10).replace(/-/g, '');

export const options = {
    scenarios: {
        ranking: {
            executor: 'constant-vus',
            vus: 50,
            duration: '10s',
        },
    },
};

export default function () {
    let res;
    if (TARGET === 'redis') {
        res = http.get(`${BASE_URL}/api/v1/rankings?date=${TODAY}&size=10&page=0`);
    } else {
        res = http.get(`${BASE_URL}/api/v1/products?sort=LIKE_COUNT&size=10&page=0`);
    }
    latency.add(res.timings.duration);
    check(res, { 'status 200': (r) => r.status === 200 });
}
