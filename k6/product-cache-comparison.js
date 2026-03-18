import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const listLatency = new Trend('product_list_latency', true);
const detailLatency = new Trend('product_detail_latency', true);

// Test configuration
// Ramp-up: 10s warm-up → 50s sustained load
export const options = {
    scenarios: {
        product_list: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 30 },  // warm-up
                { duration: '50s', target: 30 },   // sustained
            ],
            exec: 'productList',
        },
        product_detail: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 10 },
                { duration: '50s', target: 10 },
            ],
            exec: 'productDetail',
        },
    },
    thresholds: {
        errors: ['rate<0.05'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SORT_TYPES = ['LATEST', 'PRICE_ASC', 'LIKE_COUNT'];
const BRAND_IDS = [null, 1, 2, 3, 10, 30];

export function productList() {
    const sort = SORT_TYPES[Math.floor(Math.random() * SORT_TYPES.length)];
    const brandId = BRAND_IDS[Math.floor(Math.random() * BRAND_IDS.length)];
    const page = Math.floor(Math.random() * 3); // page 0~2 (cacheable range)

    let url = `${BASE_URL}/api/v1/products?sort=${sort}&page=${page}&size=20`;
    if (brandId !== null) {
        url += `&brandId=${brandId}`;
    }

    const res = http.get(url);
    listLatency.add(res.timings.duration);

    const success = check(res, {
        'list status 200': (r) => r.status === 200,
    });
    errorRate.add(!success);
    sleep(0.1);
}

export function productDetail() {
    // Top 1000 products (realistic hot data)
    const productId = Math.floor(Math.random() * 1000) + 1;

    const res = http.get(`${BASE_URL}/api/v1/products/${productId}`);
    detailLatency.add(res.timings.duration);

    const success = check(res, {
        'detail status 200 or 404': (r) => r.status === 200 || r.status === 404,
    });
    errorRate.add(!success);
    sleep(0.1);
}
