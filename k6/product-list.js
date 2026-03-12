import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { BASE_URL, LOGIN_ID, LOGIN_PW, ensureTestUser } from './helpers.js';

const popularLatency = new Trend('popular_latency', true);
const popularBrandLatency = new Trend('popular_brand_latency', true);
const priceAscLatency = new Trend('price_asc_latency', true);
const latestLatency = new Trend('latest_latency', true);
const detailLatency = new Trend('detail_latency', true);
const errorRate = new Rate('error_rate');

export const options = {
  scenarios: {
    product_list: {
      executor: 'constant-vus',
      vus: 50,
      duration: '60s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],
    error_rate: ['rate<0.01'],
  },
};

const headers = {
  'X-Loopers-LoginId': LOGIN_ID,
  'X-Loopers-LoginPw': LOGIN_PW,
};

const BRAND_IDS = [1, 5, 10, 20, 30, 40, 50];

function randomBrandId() {
  return BRAND_IDS[Math.floor(Math.random() * BRAND_IDS.length)];
}

function randomProductId() {
  return Math.floor(Math.random() * 100000) + 1;
}

function randomPage() {
  return Math.floor(Math.random() * 10);
}

export function setup() {
  ensureTestUser();
}

export default function () {
  const scenario = Math.random();

  if (scenario < 0.3) {
    // POPULAR 정렬 (30%)
    const res = http.get(
      `${BASE_URL}/api/v1/products?sort=popular&page=${randomPage()}&size=20`,
      { headers },
    );
    popularLatency.add(res.timings.duration);
    check(res, { 'popular 200': (r) => r.status === 200 });
    errorRate.add(res.status !== 200);
  } else if (scenario < 0.5) {
    // POPULAR + 브랜드 필터 (20%)
    const res = http.get(
      `${BASE_URL}/api/v1/products?sort=popular&brandId=${randomBrandId()}&page=${randomPage()}&size=20`,
      { headers },
    );
    popularBrandLatency.add(res.timings.duration);
    check(res, { 'popular+brand 200': (r) => r.status === 200 });
    errorRate.add(res.status !== 200);
  } else if (scenario < 0.7) {
    // PRICE_ASC 정렬 (20%)
    const res = http.get(
      `${BASE_URL}/api/v1/products?sort=price_asc&page=${randomPage()}&size=20`,
      { headers },
    );
    priceAscLatency.add(res.timings.duration);
    check(res, { 'price_asc 200': (r) => r.status === 200 });
    errorRate.add(res.status !== 200);
  } else if (scenario < 0.85) {
    // LATEST 정렬 (15%)
    const res = http.get(
      `${BASE_URL}/api/v1/products?sort=latest&page=${randomPage()}&size=20`,
      { headers },
    );
    latestLatency.add(res.timings.duration);
    check(res, { 'latest 200': (r) => r.status === 200 });
    errorRate.add(res.status !== 200);
  } else {
    // 상품 상세 (15%)
    const res = http.get(
      `${BASE_URL}/api/v1/products/${randomProductId()}`,
      { headers },
    );
    detailLatency.add(res.timings.duration);
    check(res, { 'detail 2xx': (r) => r.status === 200 || r.status === 404 });
    errorRate.add(res.status !== 200 && res.status !== 404);
  }

  sleep(0.1);
}
