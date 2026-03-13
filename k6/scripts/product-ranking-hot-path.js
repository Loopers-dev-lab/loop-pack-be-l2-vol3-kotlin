import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const rankingListDuration = new Trend('ranking_list_duration', true);
const productDetailDuration = new Trend('product_detail_duration', true);

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const HOT_BRAND_IDS = (__ENV.HOT_BRAND_IDS || '1,2,3,4,5')
  .split(',')
  .map((value) => Number(value.trim()))
  .filter((value) => !Number.isNaN(value));

const HOT_PRODUCT_IDS = (__ENV.HOT_PRODUCT_IDS || '1,2,3,4,5,6,7,8,9,10')
  .split(',')
  .map((value) => Number(value.trim()))
  .filter((value) => !Number.isNaN(value));

export const options = {
  scenarios: {
    ranking_list: {
      executor: 'constant-arrival-rate',
      rate: 700,
      timeUnit: '1s',
      duration: '1m',
      preAllocatedVUs: 120,
      maxVUs: 300,
      exec: 'rankingList',
    },
    product_detail: {
      executor: 'constant-arrival-rate',
      rate: 300,
      timeUnit: '1s',
      duration: '1m',
      preAllocatedVUs: 80,
      maxVUs: 200,
      exec: 'productDetail',
    },
  },
  thresholds: {
    errors: ['rate<0.05'],
    ranking_list_duration: ['p(95)<250'],
    product_detail_duration: ['p(95)<150'],
  },
};

function pickHot(array) {
  return array[Math.floor(Math.random() * array.length)];
}

export function rankingList() {
  const brandId = pickHot(HOT_BRAND_IDS);
  const res = http.get(
    `${BASE_URL}/api/v1/products?sortType=LIKES_DESC&brandId=${brandId}`,
    { headers: { 'Content-Type': 'application/json' } },
  );

  rankingListDuration.add(res.timings.duration);
  const success = check(res, {
    '[랭킹목록] status 200': (response) => response.status === 200,
  });
  errorRate.add(!success);
  sleep(0.1);
}

export function productDetail() {
  const productId = pickHot(HOT_PRODUCT_IDS);
  const res = http.get(`${BASE_URL}/api/v1/products/${productId}`, {
    headers: { 'Content-Type': 'application/json' },
  });

  productDetailDuration.add(res.timings.duration);
  const success = check(res, {
    '[상품상세] status 200': (response) => response.status === 200,
  });
  errorRate.add(!success);
  sleep(0.1);
}

