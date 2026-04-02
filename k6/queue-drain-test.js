/**
 * 대기열 드레인 부하 테스트
 *
 * 목적: k6 가상유저들이 대기열 전체 플로우(진입 → 폴링 → 토큰 → 주문)를 반복하면서
 *       대기열이 지속적으로 드레인되는 것을 확인한다.
 *
 * 수동 관찰 방법:
 *   1. 이 스크립트를 실행한다
 *   2. 별도 터미널(또는 IntelliJ HTTP Client)에서 아래 요청으로 대기열에 직접 진입한다
 *      POST http://localhost:8080/api/v1/queue/enter
 *      X-Loopers-LoginId: myuser
 *      X-Loopers-LoginPw: Test1234!
 *   3. GET /api/v1/queue/position 을 반복 호출하며 순번이 줄어드는지 확인한다
 *
 * 실행 방법:
 *   k6 run k6/queue-drain-test.js
 *
 * 사전 조건:
 *   - 서버 실행 중 (localhost:8080)
 *   - Docker infra 실행 중 (MySQL, Redis, Kafka)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'http://localhost:8080';
const JSON_HEADERS = { 'Content-Type': 'application/json' };
const ADMIN_HEADERS = {
  'Content-Type': 'application/json',
  'X-Loopers-Ldap': 'loopers.admin',
};

export const options = {
  vus: 50,        // 슬롯(5) * 10 = 항상 ~45명 대기 유지
  duration: '5m',
};

/**
 * 테스트 시작 전 1회 실행 — 브랜드 + 상품 시드 데이터 생성
 */
export function setup() {
  // 브랜드 생성
  const brandRes = http.post(
    `${BASE_URL}/api-admin/v1/brands`,
    JSON.stringify({ name: 'k6-test-brand', description: null, imageUrl: null }),
    { headers: ADMIN_HEADERS },
  );
  check(brandRes, { '브랜드 생성 성공': (r) => r.status === 200 });
  const brandId = brandRes.json('data.id');

  // 상품 생성 (재고 충분히 — 주문이 재고 부족으로 실패하지 않도록)
  const productRes = http.post(
    `${BASE_URL}/api-admin/v1/products`,
    JSON.stringify({
      brandId: brandId,
      name: 'k6-test-product',
      description: null,
      price: 10000,
      stockQuantity: 999999,
      displayYn: true,
      imageUrl: null,
    }),
    { headers: ADMIN_HEADERS },
  );
  check(productRes, { '상품 생성 성공': (r) => r.status === 200 });
  const productId = productRes.json('data.id');

  console.log(`[setup] brandId=${brandId}, productId=${productId}`);
  return { productId };
}

/**
 * VU별 반복 실행 — 대기열 전체 플로우
 * __VU: 1~50 고정 (VU 번호), __ITER: 반복 횟수
 */
export default function (data) {
  const loginId = `k6user${__VU}`;
  const password = 'Test1234!';
  const authHeaders = {
    'Content-Type': 'application/json',
    'X-Loopers-LoginId': loginId,
    'X-Loopers-LoginPw': password,
  };

  // 1. 회원가입 (첫 번째 iteration에만 성공, 이후는 409 — 무시하고 진행)
  if (__ITER === 0) {
    http.post(
      `${BASE_URL}/api/v1/users`,
      JSON.stringify({
        loginId: loginId,
        password: password,
        name: `k6user${__VU}`,
        birthday: '1990-01-01',
        email: `${loginId}@k6test.com`,  // loginId = k6user{N} (underscore 제거 — loginId 영숫자만 허용)
      }),
      { headers: JSON_HEADERS },
    );
  }

  // 2. 대기열 진입
  const enterRes = http.post(`${BASE_URL}/api/v1/queue/enter`, null, { headers: authHeaders });
  check(enterRes, { '대기열 진입 성공': (r) => r.status === 200 });

  // 3. 토큰 발급될 때까지 폴링 (retryAfterMs 기반으로 대기)
  let token = null;
  for (let i = 0; i < 300; i++) {
    const posRes = http.get(`${BASE_URL}/api/v1/queue/position`, { headers: authHeaders });
    if (!posRes || posRes.status === 0) { sleep(1); continue; } // 연결 실패 시 스킵

    let body = null;
    try { body = posRes.json('data'); } catch (_) {}

    if (body && body.token) {
      token = body.token;
      break;
    }

    const retryAfterMs = (body && body.retryAfterMs) || 500;
    sleep(retryAfterMs / 1000);
  }

  if (!token) {
    console.warn(`[VU${__VU}] 토큰 수령 실패 (timeout)`);
    return;
  }

  // 4. 실제 유저처럼 주문 페이지에서 몇 초 머무는 시간 시뮬레이션
  //    이 sleep이 없으면 토큰을 즉시 반납해서 대기열이 쌓이지 않음
  sleep(Math.random() * 5 + 15); // 15~20초 대기

  // 5. 주문 (토큰 소비 → 대기열 슬롯 해방)
  const orderRes = http.post(
    `${BASE_URL}/api/v1/orders`,
    JSON.stringify({
      items: [{ productId: data.productId, quantity: 1 }],
      entryToken: token,
    }),
    { headers: authHeaders },
  );
  check(orderRes, { '주문 성공': (r) => r.status === 200 });

  // 6. 주문 후 대기 — 슬롯을 바로 재점유하지 않아야 대기열 유저가 승급됨
  //    이 sleep이 없으면 VU가 즉시 재진입해서 열린 슬롯을 낚아채 대기열이 드레인 안 됨
  sleep(Math.random() * 5 + 15); // 15~20초 대기 후 루프 재시작
}
