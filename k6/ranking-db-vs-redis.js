import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

/**
 * DB vs Redis 랭킹 조회 성능 비교
 *
 * 사전 조건:
 *   1. 서버 기동 (commerce-api)
 *   2. 테스트 데이터 삽입 (상품 + Redis ZSET 점수)
 *
 * 실행:
 *   k6 run k6/ranking-db-vs-redis.js
 */

const redisLatency = new Trend('redis_ranking_latency', true);
const dbLatency = new Trend('db_ranking_latency', true);

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TODAY = new Date().toISOString().slice(0, 10).replace(/-/g, '');

export const options = {
    scenarios: {
        redis_ranking: {
            executor: 'constant-vus',
            vus: 50,
            duration: '30s',
            exec: 'redisRanking',
        },
        db_product_list: {
            executor: 'constant-vus',
            vus: 50,
            duration: '30s',
            exec: 'dbProductList',
            startTime: '35s',
        },
    },
};

export function redisRanking() {
    const res = http.get(`${BASE_URL}/api/v1/rankings?date=${TODAY}&size=10&page=0`);
    redisLatency.add(res.timings.duration);
    check(res, { 'redis status 200': (r) => r.status === 200 });
}

export function dbProductList() {
    const res = http.get(`${BASE_URL}/api/v1/products?sort=LIKE_COUNT&size=10&page=0`);
    dbLatency.add(res.timings.duration);
    check(res, { 'db status 200': (r) => r.status === 200 });
}
