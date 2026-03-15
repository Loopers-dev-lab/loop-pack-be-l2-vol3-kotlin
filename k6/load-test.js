import http from "k6/http";
import { check, sleep, group } from "k6";
import { Counter, Trend } from "k6/metrics";

// ── 설정 ──────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const PASSWORD = "Password1!";

// 커스텀 메트릭
const orderSuccess = new Counter("order_success");
const orderFail = new Counter("order_fail");
const couponIssueSuccess = new Counter("coupon_issue_success");
const couponIssueFail = new Counter("coupon_issue_fail");
const likeSuccess = new Counter("like_success");
const likeFail = new Counter("like_fail");

// ── 시나리오 ──────────────────────────────────────────
export const options = {
  scenarios: {
    // 1) 재고 차감 동시성: 50명이 동시에 주문
    stock_concurrency: {
      executor: "per-vu-iterations",
      vus: 50,
      iterations: 1,
      startTime: "0s",
      maxDuration: "30s",
      exec: "stockTest",
      tags: { scenario: "stock" },
    },
    // 2) 쿠폰 발급 동시성: 100명이 한정 쿠폰(50장) 동시 발급
    coupon_concurrency: {
      executor: "per-vu-iterations",
      vus: 100,
      iterations: 1,
      startTime: "35s",
      exec: "couponTest",
      maxDuration: "30s",
      tags: { scenario: "coupon" },
    },
    // 3) 좋아요 동시성: 50명이 같은 상품에 동시 좋아요
    like_concurrency: {
      executor: "per-vu-iterations",
      vus: 50,
      iterations: 1,
      startTime: "70s",
      exec: "likeTest",
      maxDuration: "30s",
      tags: { scenario: "like" },
    },
  },
  thresholds: {
    http_req_duration: ["p(95)<3000"],
  },
};

// ── 헬퍼 ──────────────────────────────────────────────
function authHeaders(loginId) {
  return {
    "Content-Type": "application/json",
    "X-Loopers-LoginId": loginId,
    "X-Loopers-LoginPw": PASSWORD,
  };
}

function adminHeaders() {
  return { "Content-Type": "application/json" };
}

function registerUser(loginId) {
  const res = http.post(
    `${BASE_URL}/api/v1/users`,
    JSON.stringify({
      loginId: loginId,
      password: PASSWORD,
      name: "부하테스트",
      birthDate: "1993-04-01",
      email: `${loginId}@test.com`,
      gender: "MALE",
    }),
    { headers: { "Content-Type": "application/json" } }
  );
  return res;
}

function createBrand() {
  const res = http.post(
    `${BASE_URL}/api-admin/v1/brands`,
    JSON.stringify({
      name: `테스트브랜드_${Date.now()}`,
      description: null,
      logoUrl: null,
    }),
    { headers: adminHeaders() }
  );
  return JSON.parse(res.body).data.id;
}

function createProduct(brandId, stock) {
  const res = http.post(
    `${BASE_URL}/api-admin/v1/products`,
    JSON.stringify({
      brandId: brandId,
      name: `부하테스트상품_${Date.now()}`,
      description: "부하테스트용",
      price: 10000,
      stock: stock,
      thumbnailUrl: null,
      images: [],
    }),
    { headers: adminHeaders() }
  );
  return JSON.parse(res.body).data.id;
}

function createCoupon(maxIssueCount) {
  const expiredAt = new Date(Date.now() + 86400000 * 30).toISOString();
  const res = http.post(
    `${BASE_URL}/api-admin/v1/coupons`,
    JSON.stringify({
      name: `부하테스트쿠폰_${Date.now()}`,
      discountType: "FIXED",
      discountValue: 1000,
      minOrderAmount: 0,
      maxIssueCount: maxIssueCount,
      expiredAt: expiredAt,
    }),
    { headers: adminHeaders() }
  );
  return JSON.parse(res.body).data.id;
}

// ── setup: 테스트 데이터 준비 ─────────────────────────
export function setup() {
  console.log("=== Setup: 테스트 데이터 준비 ===");

  const brandId = createBrand();
  console.log(`브랜드 생성: ${brandId}`);

  // 시나리오 1: 재고 테스트용 - 재고 30개, 50명 주문 → 30명만 성공 예상
  const stockProductId = createProduct(brandId, 30);
  console.log(`재고 테스트 상품 생성: ${stockProductId} (재고: 30)`);

  // 시나리오 1 유저 등록
  for (let i = 1; i <= 50; i++) {
    registerUser(`stk${String(i).padStart(3, "0")}`);
  }
  console.log("재고 테스트 유저 50명 등록 완료");

  // 시나리오 2: 쿠폰 테스트용 - 50장 한정, 100명 발급 시도 → 50명만 성공 예상
  const couponId = createCoupon(50);
  console.log(`쿠폰 생성: ${couponId} (한정: 50장)`);

  // 시나리오 2 유저 등록
  for (let i = 1; i <= 100; i++) {
    registerUser(`cpn${String(i).padStart(3, "0")}`);
  }
  console.log("쿠폰 테스트 유저 100명 등록 완료");

  // 시나리오 3: 좋아요 테스트용
  const likeProductId = createProduct(brandId, 100);
  console.log(`좋아요 테스트 상품 생성: ${likeProductId}`);

  for (let i = 1; i <= 50; i++) {
    registerUser(`lik${String(i).padStart(3, "0")}`);
  }
  console.log("좋아요 테스트 유저 50명 등록 완료");

  console.log("=== Setup 완료 ===\n");

  return {
    brandId,
    stockProductId,
    couponId,
    likeProductId,
  };
}

// ── 시나리오 1: 재고 동시성 ───────────────────────────
export function stockTest(data) {
  const vuId = __VU;
  const loginId = `stk${String(vuId).padStart(3, "0")}`;

  const res = http.post(
    `${BASE_URL}/api/v1/orders`,
    JSON.stringify({
      items: [{ productId: data.stockProductId, quantity: 1 }],
    }),
    { headers: authHeaders(loginId) }
  );

  const success = check(res, {
    "주문 응답 수신": (r) => r.status === 201 || r.status === 400 || r.status === 409 || r.status === 500,
  });

  if (res.status === 201) {
    orderSuccess.add(1);
  } else {
    orderFail.add(1);
  }
}

// ── 시나리오 2: 쿠폰 발급 동시성 ─────────────────────
export function couponTest(data) {
  const vuId = __VU;
  const loginId = `cpn${String(vuId).padStart(3, "0")}`;

  const res = http.post(
    `${BASE_URL}/api/v1/coupons/${data.couponId}/issue`,
    null,
    { headers: authHeaders(loginId) }
  );

  const success = check(res, {
    "쿠폰 발급 응답 수신": (r) => r.status === 201 || r.status === 400 || r.status === 409,
  });

  if (res.status === 201) {
    couponIssueSuccess.add(1);
  } else {
    couponIssueFail.add(1);
  }
}

// ── 시나리오 3: 좋아요 동시성 ─────────────────────────
export function likeTest(data) {
  const vuId = __VU;
  const loginId = `lik${String(vuId).padStart(3, "0")}`;

  const res = http.post(
    `${BASE_URL}/api/v1/products/${data.likeProductId}/likes`,
    null,
    { headers: authHeaders(loginId) }
  );

  const success = check(res, {
    "좋아요 응답 수신": (r) => r.status === 200 || r.status === 409,
  });

  if (res.status === 200) {
    likeSuccess.add(1);
  } else {
    likeFail.add(1);
  }
}

// ── teardown ──────────────────────────────────────────
export function teardown(data) {
  console.log("\n=== 부하테스트 결과 요약 ===");
  console.log(`재고 테스트: 상품 ID ${data.stockProductId} (재고 30개, 50명 주문)`);
  console.log(`쿠폰 테스트: 쿠폰 ID ${data.couponId} (50장 한정, 100명 발급)`);
  console.log(`좋아요 테스트: 상품 ID ${data.likeProductId} (50명 동시 좋아요)`);
  console.log("커스텀 메트릭에서 성공/실패 카운트를 확인하세요.");
  console.log("===========================\n");
}
