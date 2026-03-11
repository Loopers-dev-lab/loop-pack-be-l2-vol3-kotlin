import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// --- Custom Metrics ---
const errorRate = new Rate('errors');
const productListDuration = new Trend('product_list_duration', true);
const productDetailDuration = new Trend('product_detail_duration', true);
const orderCreateDuration = new Trend('order_create_duration', true);

// --- Config ---
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// --- Scenarios ---
// 목표: 전체 TPS ~1000
//   - 상품 목록 조회 (60%): 600 TPS
//   - 상품 상세 조회 (25%): 250 TPS
//   - 주문 생성 (15%):      150 TPS
export const options = {
  scenarios: {
    // 1) 상품 목록 조회 (Read-heavy, 인증 불필요)
    product_list: {
      executor: 'constant-arrival-rate',
      rate: 600,
      timeUnit: '1s',
      duration: '1m',
      preAllocatedVUs: 100,
      maxVUs: 300,
      exec: 'productList',
    },

    // 2) 상품 상세 조회 (Read, 인증 불필요)
    product_detail: {
      executor: 'constant-arrival-rate',
      rate: 250,
      timeUnit: '1s',
      duration: '1m',
      preAllocatedVUs: 50,
      maxVUs: 150,
      exec: 'productDetail',
    },

    // 3) 주문 생성 (Write, 인증 필요, 재고 차감)
    order_create: {
      executor: 'constant-arrival-rate',
      rate: 150,
      timeUnit: '1s',
      duration: '1m',
      preAllocatedVUs: 50,
      maxVUs: 200,
      exec: 'orderCreate',
    },
  },

  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    errors: ['rate<0.1'],
    product_list_duration: ['p(95)<300'],
    product_detail_duration: ['p(95)<200'],
    order_create_duration: ['p(95)<1000'],
  },
};

const headers = { 'Content-Type': 'application/json' };

function authHeaders(userId) {
  return {
    ...headers,
    'X-Loopers-LoginId': `testuser${userId}`,
    'X-Loopers-LoginPw': `Password${userId}!`,
  };
}

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

// --- Scenario Functions ---

export function productList() {
  const res = http.get(`${BASE_URL}/api/v1/products`, { headers });

  productListDuration.add(res.timings.duration);
  const success = check(res, {
    '[상품목록] status 200': (r) => r.status === 200,
  });
  errorRate.add(!success);
}

export function productDetail() {
  const productId = randomInt(2, 11);
  const res = http.get(`${BASE_URL}/api/v1/products/${productId}`, { headers });

  productDetailDuration.add(res.timings.duration);
  const success = check(res, {
    '[상품상세] status 200': (r) => r.status === 200,
  });
  errorRate.add(!success);
}

export function orderCreate() {
  const userId = randomInt(1, 100);
  const productId = randomInt(2, 11);
  const quantity = randomInt(1, 3);

  const payload = JSON.stringify({
    items: [{ productId, quantity }],
  });

  const res = http.post(`${BASE_URL}/api/v1/orders`, payload, {
    headers: authHeaders(userId),
  });

  orderCreateDuration.add(res.timings.duration);
  const success = check(res, {
    '[주문생성] status 200': (r) => r.status === 200,
  });
  errorRate.add(!success);
}
