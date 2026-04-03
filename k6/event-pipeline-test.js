import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Trend } from "k6/metrics";

// ── 설정 ──────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || "http://host.docker.internal:8080";
const PASSWORD = "Password1!";

// 커스텀 메트릭
const viewEvents = new Counter("view_events_published");
const likeEvents = new Counter("like_events_published");
const orderEvents = new Counter("order_events_published");
const viewLatency = new Trend("product_view_latency", true);
const likeLatency = new Trend("like_latency", true);
const orderLatency = new Trend("order_latency", true);

// ── 시나리오 ──────────────────────────────────────────
export const options = {
  scenarios: {
    // Phase 1: 상품 조회 이벤트 (view → Kafka → product_metrics.view_count)
    product_view_load: {
      executor: "ramping-vus",
      stages: [
        { duration: "5s", target: 10 },
        { duration: "20s", target: 50 },
        { duration: "10s", target: 50 },
        { duration: "5s", target: 0 },
      ],
      exec: "productViewTest",
      startTime: "0s",
      tags: { scenario: "product_view" },
    },
    // Phase 2: 좋아요 이벤트 (like → Kafka → product_metrics.like_count)
    like_load: {
      executor: "ramping-vus",
      stages: [
        { duration: "5s", target: 5 },
        { duration: "15s", target: 20 },
        { duration: "10s", target: 0 },
      ],
      exec: "likeEventTest",
      startTime: "45s",
      tags: { scenario: "like_event" },
    },
    // Phase 3: 주문 이벤트 (order → Kafka → order-events)
    order_load: {
      executor: "ramping-vus",
      stages: [
        { duration: "5s", target: 5 },
        { duration: "15s", target: 10 },
        { duration: "10s", target: 0 },
      ],
      exec: "orderEventTest",
      startTime: "80s",
      tags: { scenario: "order_event" },
    },
  },
  thresholds: {
    http_req_duration: ["p(95)<3000"],
    "product_view_latency": ["p(95)<500"],
    "like_latency": ["p(95)<1000"],
    "order_latency": ["p(95)<2000"],
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
  return http.post(
    `${BASE_URL}/api/v1/users`,
    JSON.stringify({
      loginId: loginId,
      password: PASSWORD,
      name: "이벤트테스트",
      birthDate: "1993-04-01",
      email: `${loginId}@test.com`,
      gender: "MALE",
    }),
    { headers: { "Content-Type": "application/json" } },
  );
}

function createBrand() {
  const res = http.post(
    `${BASE_URL}/api-admin/v1/brands`,
    JSON.stringify({
      name: `이벤트테스트브랜드_${Date.now()}`,
      description: null,
      logoUrl: null,
    }),
    { headers: adminHeaders() },
  );
  return JSON.parse(res.body).data.id;
}

function createProduct(brandId, stock) {
  const res = http.post(
    `${BASE_URL}/api-admin/v1/products`,
    JSON.stringify({
      brandId: brandId,
      name: `이벤트테스트상품_${Date.now()}_${Math.random()}`,
      description: "이벤트 파이프라인 부하테스트용",
      price: 10000,
      stock: stock,
      thumbnailUrl: null,
      images: [],
    }),
    { headers: adminHeaders() },
  );
  return JSON.parse(res.body).data.id;
}

// ── setup ─────────────────────────────────────────────
export function setup() {
  console.log("=== Setup: 이벤트 파이프라인 부하테스트 ===");

  const brandId = createBrand();
  console.log(`브랜드 생성: ${brandId}`);

  // 조회 테스트용 상품 5개
  const viewProducts = [];
  for (let i = 0; i < 5; i++) {
    const pid = createProduct(brandId, 100);
    viewProducts.push(pid);
  }
  console.log(`조회 테스트 상품 ${viewProducts.length}개 생성`);

  // 좋아요 테스트용 상품
  const likeProductId = createProduct(brandId, 100);
  console.log(`좋아요 테스트 상품: ${likeProductId}`);

  // 주문 테스트용 상품 (재고 넉넉하게)
  const orderProductId = createProduct(brandId, 10000);
  console.log(`주문 테스트 상품: ${orderProductId} (재고: 10000)`);

  // 유저 등록
  for (let i = 1; i <= 60; i++) {
    registerUser(`evt${String(i).padStart(3, "0")}`);
  }
  console.log("유저 60명 등록 완료");

  console.log("=== Setup 완료 ===\n");
  return { brandId, viewProducts, likeProductId, orderProductId };
}

// ── 시나리오 1: 상품 조회 이벤트 부하 ─────────────────
export function productViewTest(data) {
  const productId =
    data.viewProducts[Math.floor(Math.random() * data.viewProducts.length)];

  const start = Date.now();
  const res = http.get(`${BASE_URL}/api/v1/products/${productId}`);
  viewLatency.add(Date.now() - start);

  check(res, {
    "상품 조회 성공": (r) => r.status === 200,
  });

  if (res.status === 200) {
    viewEvents.add(1);
  }

  sleep(0.1);
}

// ── 시나리오 2: 좋아요 이벤트 부하 ────────────────────
export function likeEventTest(data) {
  const vuId = (__VU % 50) + 1;
  const loginId = `evt${String(vuId).padStart(3, "0")}`;

  const start = Date.now();
  const res = http.post(
    `${BASE_URL}/api/v1/products/${data.likeProductId}/likes`,
    null,
    { headers: authHeaders(loginId) },
  );
  likeLatency.add(Date.now() - start);

  check(res, {
    "좋아요 응답": (r) => r.status === 200 || r.status === 409,
  });

  if (res.status === 200) {
    likeEvents.add(1);
  }

  sleep(0.5);
}

// ── 시나리오 3: 주문 이벤트 부하 ──────────────────────
export function orderEventTest(data) {
  const vuId = (__VU % 50) + 1;
  const loginId = `evt${String(vuId).padStart(3, "0")}`;

  const start = Date.now();
  const res = http.post(
    `${BASE_URL}/api/v1/orders`,
    JSON.stringify({
      items: [{ productId: data.orderProductId, quantity: 1 }],
    }),
    { headers: authHeaders(loginId) },
  );
  orderLatency.add(Date.now() - start);

  check(res, {
    "주문 응답": (r) => r.status === 201 || r.status === 409,
  });

  if (res.status === 201) {
    orderEvents.add(1);
  }

  sleep(0.5);
}

// ── teardown ──────────────────────────────────────────
export function teardown(data) {
  console.log("\n=== 이벤트 파이프라인 부하테스트 결과 ===");
  console.log("커스텀 메트릭:");
  console.log("  view_events_published: 상품 조회 이벤트 수");
  console.log("  like_events_published: 좋아요 이벤트 수");
  console.log("  order_events_published: 주문 이벤트 수");
  console.log("  product_view_latency: 상품 조회 응답 시간");
  console.log("  like_latency: 좋아요 응답 시간");
  console.log("  order_latency: 주문 응답 시간");
  console.log("");
  console.log("검증 포인트:");
  console.log("  1. Outbox relay가 이벤트를 Kafka로 정상 발행하는지 (Kafka-UI 확인)");
  console.log("  2. commerce-streamer가 product_metrics를 정상 upsert하는지 (DB 확인)");
  console.log("  3. event_handled 테이블에 멱등 처리 기록이 남는지");
  console.log("  4. Outbox 테이블의 PENDING → PUBLISHED 전이가 정상인지");
  console.log("============================================\n");
}
