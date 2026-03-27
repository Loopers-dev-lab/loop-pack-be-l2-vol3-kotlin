import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const detailLatency = new Trend('product_detail_latency');
const listLatency = new Trend('product_list_latency');
const errorRate = new Rate('errors');

export const options = {
    scenarios: {
        mixed_load: {
            executor: 'constant-vus',
            vus: 100,
            duration: '30s',
        },
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
    if (Math.random() < 0.7) {
        // 70% 목록 조회
        const brandId = Math.ceil(Math.random() * 20);
        const page = Math.floor(Math.random() * 5);
        const sorts = ['LATEST', 'PRICE_ASC', 'LIKES_DESC'];
        const sort = sorts[Math.floor(Math.random() * 3)];

        const res = http.get(
            `${BASE_URL}/api/v1/products?brandId=${brandId}&sort=${sort}&page=${page}&size=20`
        );
        listLatency.add(res.timings.duration);
        check(res, { 'list 200': (r) => r.status === 200 });
        errorRate.add(res.status !== 200);
    } else {
        // 30% 상세 조회
        const productId = Math.ceil(Math.random() * 100000);
        const res = http.get(`${BASE_URL}/api/v1/products/${productId}`);
        detailLatency.add(res.timings.duration);
        check(res, { 'detail 200': (r) => r.status === 200 });
        errorRate.add(res.status !== 200);
    }

    sleep(0.1);
}
