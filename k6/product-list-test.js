import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const listLatency = new Trend('product_list_latency', true);
const detailLatency = new Trend('product_detail_latency', true);

// Test configuration
export const options = {
    scenarios: {
        product_list: {
            executor: 'constant-vus',
            vus: 30,
            duration: '1m',
            exec: 'productList',
        },
        product_detail: {
            executor: 'constant-vus',
            vus: 10,
            duration: '1m',
            exec: 'productDetail',
        },
    },
    thresholds: {
        'product_list_latency{quantile:0.90}': ['value<500'],
        'product_list_latency{quantile:0.99}': ['value<2000'],
        errors: ['rate<0.01'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SORT_TYPES = ['LATEST', 'PRICE_ASC', 'LIKE_COUNT'];
const BRAND_IDS = [null, 1, 2, 3, 10, 30]; // null = no filter

export function productList() {
    const sort = SORT_TYPES[Math.floor(Math.random() * SORT_TYPES.length)];
    const brandId = BRAND_IDS[Math.floor(Math.random() * BRAND_IDS.length)];
    const page = Math.floor(Math.random() * 5); // page 0~4

    let url = `${BASE_URL}/api/v1/products?sort=${sort}&page=${page}&size=20`;
    if (brandId !== null) {
        url += `&brandId=${brandId}`;
    }

    const res = http.get(url);
    listLatency.add(res.timings.duration);

    const success = check(res, {
        'status is 200': (r) => r.status === 200,
        'has content': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.data && body.data.content !== undefined;
            } catch {
                return false;
            }
        },
    });
    errorRate.add(!success);
    sleep(0.1);
}

export function productDetail() {
    // Random product ID between 1 and 100000
    const productId = Math.floor(Math.random() * 100000) + 1;

    const res = http.get(`${BASE_URL}/api/v1/products/${productId}`);
    detailLatency.add(res.timings.duration);

    const success = check(res, {
        'status is 200 or 404': (r) => r.status === 200 || r.status === 404,
    });
    errorRate.add(!success);
    sleep(0.1);
}
