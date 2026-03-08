import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";
import exec from "k6/execution";

// ── 설정 ──────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const PASSWORD = "Password1!";
const MAX_VUS = parseInt(__ENV.MAX_VUS || "10000");
const BATCH_SIZE = 100;

// ── 커스텀 메트릭 ─────────────────────────────────────
const orderSuccess = new Counter("order_success");
const orderFail = new Counter("order_fail");
const couponSuccess = new Counter("coupon_success");
const couponFail = new Counter("coupon_fail");
const likeSuccess = new Counter("like_success");
const likeFail = new Counter("like_fail");
const orderDuration = new Trend("order_duration", true);
const couponDuration = new Trend("coupon_duration", true);
const likeDuration = new Trend("like_duration", true);
const errorRate = new Rate("error_rate");

// ── 시나리오: 100 → 1000 → 10000 단계적 증가 ─────────
export const options = {
  setupTimeout: "600s",
  scenarios: {
    ramping_load: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        // Stage 1: 100 VU
        { duration: "10s", target: 100 },  // ramp up
        { duration: "30s", target: 100 },  // hold
        // Stage 2: 1000 VU
        { duration: "20s", target: 1000 }, // ramp up
        { duration: "30s", target: 1000 }, // hold
        // Stage 3: 10000 VU
        { duration: "30s", target: MAX_VUS }, // ramp up
        { duration: "30s", target: MAX_VUS }, // hold
        // Ramp down
        { duration: "10s", target: 0 },
      ],
      gracefulRampDown: "10s",
    },
  },
  thresholds: {
    http_req_duration: ["p(95)<5000"],
    error_rate: ["rate<0.5"],
  },
};

// ── 헬퍼 ──────────────────────────────────────────────
function headers() {
  return { "Content-Type": "application/json" };
}

function authHeaders(loginId) {
  return {
    "Content-Type": "application/json",
    "X-Loopers-LoginId": loginId,
    "X-Loopers-LoginPw": PASSWORD,
  };
}

function pad(n) {
  return String(n).padStart(5, "0");
}

// ── setup: 테스트 데이터 대량 준비 ────────────────────
export function setup() {
  const startTime = Date.now();
  console.log(`=== Setup 시작: ${MAX_VUS}명 유저 준비 ===`);

  // 브랜드 생성
  const brandRes = http.post(
    `${BASE_URL}/api-admin/v1/brands`,
    JSON.stringify({ name: `부하브랜드_${Date.now()}`, description: null, logoUrl: null }),
    { headers: headers() }
  );
  const brandId = JSON.parse(brandRes.body).data.id;
  console.log(`브랜드: ${brandId}`);

  // 상품 생성 (재고 충분히)
  const orderProductId = createProduct(brandId, MAX_VUS * 2);
  console.log(`주문용 상품: ${orderProductId} (재고: ${MAX_VUS * 2})`);

  const likeProductId = createProduct(brandId, 1);
  console.log(`좋아요용 상품: ${likeProductId}`);

  // 쿠폰 생성 (발급 수량의 절반만 → 경쟁 유도)
  const couponId = createCoupon(Math.floor(MAX_VUS / 2));
  console.log(`쿠폰: ${couponId} (한정: ${Math.floor(MAX_VUS / 2)}장)`);

  // 유저 대량 등록 (배치)
  let created = 0;
  for (let i = 1; i <= MAX_VUS; i += BATCH_SIZE) {
    const batch = [];
    for (let j = i; j < i + BATCH_SIZE && j <= MAX_VUS; j++) {
      batch.push([
        "POST",
        `${BASE_URL}/api/v1/users`,
        JSON.stringify({
          loginId: `u${pad(j)}`,
          password: PASSWORD,
          name: "부하유저",
          birthDate: "1993-04-01",
          email: `u${pad(j)}@t.com`,
          gender: "MALE",
        }),
        { headers: headers() },
      ]);
    }
    http.batch(batch);
    created += batch.length;
    if (created % 500 === 0 || created >= MAX_VUS) {
      console.log(`유저 등록: ${created}/${MAX_VUS}`);
    }
  }

  const elapsed = ((Date.now() - startTime) / 1000).toFixed(1);
  console.log(`=== Setup 완료 (${elapsed}s) ===\n`);

  return { orderProductId, likeProductId, couponId };
}

// ── 메인 시나리오: 각 VU가 3가지 동시성 API 호출 ──────
export default function (data) {
  const vuId = exec.vu.idInTest;
  const loginId = `u${pad(vuId)}`;

  // 1) 주문 (재고 차감)
  {
    const res = http.post(
      `${BASE_URL}/api/v1/orders`,
      JSON.stringify({
        items: [{ productId: data.orderProductId, quantity: 1 }],
      }),
      { headers: authHeaders(loginId) }
    );
    orderDuration.add(res.timings.duration);
    if (res.status === 201) {
      orderSuccess.add(1);
      errorRate.add(false);
    } else {
      orderFail.add(1);
      errorRate.add(true);
    }
    check(res, {
      "주문 응답": (r) => r.status === 201 || r.status === 400 || r.status === 409 || r.status === 500,
    });
  }

  sleep(0.1);

  // 2) 쿠폰 발급
  {
    const res = http.post(
      `${BASE_URL}/api/v1/coupons/${data.couponId}/issue`,
      null,
      { headers: authHeaders(loginId) }
    );
    couponDuration.add(res.timings.duration);
    if (res.status === 201) {
      couponSuccess.add(1);
      errorRate.add(false);
    } else {
      couponFail.add(1);
      errorRate.add(true);
    }
    check(res, {
      "쿠폰 발급 응답": (r) => r.status === 201 || r.status === 400 || r.status === 409,
    });
  }

  sleep(0.1);

  // 3) 좋아요
  {
    const res = http.post(
      `${BASE_URL}/api/v1/products/${data.likeProductId}/likes`,
      null,
      { headers: authHeaders(loginId) }
    );
    likeDuration.add(res.timings.duration);
    if (res.status === 200) {
      likeSuccess.add(1);
      errorRate.add(false);
    } else {
      likeFail.add(1);
      errorRate.add(true);
    }
    check(res, {
      "좋아요 응답": (r) => r.status === 200 || r.status === 409,
    });
  }

  sleep(0.5);
}

// ── teardown ──────────────────────────────────────────
export function teardown(data) {
  console.log("\n=== 부하테스트 완료 ===");
  console.log("커스텀 메트릭 요약:");
  console.log("  order_success / order_fail    → 재고 차감 동시성");
  console.log("  coupon_success / coupon_fail   → 쿠폰 발급 동시성");
  console.log("  like_success / like_fail      → 좋아요 동시성");
  console.log("  order/coupon/like_duration     → 각 API 응답시간");
  console.log("========================\n");
}

// ── 내부 함수 ─────────────────────────────────────────
function createProduct(brandId, stock) {
  const res = http.post(
    `${BASE_URL}/api-admin/v1/products`,
    JSON.stringify({
      brandId, name: `부하상품_${Date.now()}`, description: "부하테스트",
      price: 10000, stock, thumbnailUrl: null, images: [],
    }),
    { headers: headers() }
  );
  return JSON.parse(res.body).data.id;
}

function createCoupon(maxIssueCount) {
  const expiredAt = new Date(Date.now() + 86400000 * 30).toISOString();
  const res = http.post(
    `${BASE_URL}/api-admin/v1/coupons`,
    JSON.stringify({
      name: `부하쿠폰_${Date.now()}`, discountType: "FIXED",
      discountValue: 1000, minOrderAmount: 0, maxIssueCount, expiredAt,
    }),
    { headers: headers() }
  );
  return JSON.parse(res.body).data.id;
}
