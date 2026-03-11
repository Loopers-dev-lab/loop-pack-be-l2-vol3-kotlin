import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 재고 동시성 스트레스 테스트
// 같은 상품에 동시 주문 → Atomic UPDATE가 정확히 동작하는지 확인
// 상품 재고 100개, 200명이 동시에 1개씩 주문 → 100명 성공, 100명 실패 예상

const orderSuccess = new Counter('order_success');
const orderFail = new Counter('order_fail');

export const options = {
  scenarios: {
    stock_race: {
      executor: 'shared-iterations',
      vus: 200,
      iterations: 200,
      maxDuration: '30s',
    },
  },
};

const headers = { 'Content-Type': 'application/json' };

function authHeaders(userId) {
  return {
    ...headers,
    'X-Loopers-LoginId': `testuser${userId}`,
    'X-Loopers-LoginPw': `password${userId}!`,
  };
}

// setup: 재고 100개 상품 생성
export function setup() {
  const adminHeaders = {
    ...headers,
    'X-Loopers-LoginId': 'admin',
    'X-Loopers-LoginPw': 'admin123!',
  };

  // 브랜드 조회 (이미 있다고 가정)
  const brandRes = http.get(`${BASE_URL}/api/v1/brands`, { headers });
  const brandId = brandRes.json('data.0.id') || 1;

  // 재고 100개짜리 상품 생성
  const productRes = http.post(
    `${BASE_URL}/api/v1/products`,
    JSON.stringify({
      brandId: brandId,
      name: '재고스트레스테스트상품',
      price: 10000,
      description: '동시성 테스트용',
      stock: 100,
    }),
    { headers: adminHeaders },
  );

  const productId = productRes.json('data.id');
  console.log(`Created product ${productId} with stock=100`);
  return { productId };
}

export default function (data) {
  const userId = (__VU % 100) + 1;
  const payload = JSON.stringify({
    items: [{ productId: data.productId, quantity: 1 }],
  });

  const res = http.post(`${BASE_URL}/api/v1/orders`, payload, {
    headers: authHeaders(userId),
  });

  if (res.status === 200) {
    orderSuccess.add(1);
  } else {
    orderFail.add(1);
  }

  check(res, {
    'status is 200 or expected error': (r) => r.status === 200 || r.status === 400,
  });
}

export function teardown(data) {
  // 결과 확인: 상품 재고 조회
  const res = http.get(`${BASE_URL}/api/v1/products/${data.productId}`, {
    headers: { 'Content-Type': 'application/json' },
  });
  const stock = res.json('data.stock');
  console.log(`Final stock: ${stock} (expected: 0)`);
  console.log('Check order_success counter = 100, order_fail counter = 100');
}
