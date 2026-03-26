import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Trend } from "k6/metrics";

// ── 설정 ──────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || "http://host.docker.internal:8080";
const PASSWORD = "Password1!";

// 커스텀 메트릭
const issueRequested = new Counter("coupon_issue_requested");
const issueSuccess = new Counter("coupon_issue_success");
const issueFail = new Counter("coupon_issue_fail");
const issuePending = new Counter("coupon_issue_pending");
const pollingDuration = new Trend("coupon_polling_duration", true);

// ── 시나리오 ──────────────────────────────────────────
export const options = {
  scenarios: {
    // 선착순 100장 쿠폰에 500명 동시 요청
    coupon_async_burst: {
      executor: "per-vu-iterations",
      vus: 500,
      iterations: 1,
      maxDuration: "60s",
      exec: "couponAsyncTest",
      startTime: "0s",
      tags: { scenario: "coupon_async" },
    },
    // 발급 결과 polling (burst 후 10초 대기 → polling 시작)
    coupon_polling: {
      executor: "per-vu-iterations",
      vus: 500,
      iterations: 1,
      maxDuration: "60s",
      exec: "couponPollingTest",
      startTime: "15s",
      tags: { scenario: "coupon_polling" },
    },
  },
  thresholds: {
    http_req_duration: ["p(95)<5000"],
    "http_req_duration{scenario:coupon_async}": ["p(95)<2000"],
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
      name: "쿠폰테스트",
      birthDate: "1993-04-01",
      email: `${loginId}@test.com`,
      gender: "MALE",
    }),
    { headers: { "Content-Type": "application/json" } },
  );
}

function createCoupon(maxIssueCount) {
  const expiredAt = new Date(Date.now() + 86400000 * 30).toISOString();
  const res = http.post(
    `${BASE_URL}/api-admin/v1/coupons`,
    JSON.stringify({
      name: `선착순쿠폰_${Date.now()}`,
      discountType: "FIXED",
      discountValue: 1000,
      minOrderAmount: 0,
      maxIssueCount: maxIssueCount,
      expiredAt: expiredAt,
    }),
    { headers: adminHeaders() },
  );
  return JSON.parse(res.body).data.id;
}

// ── setup ─────────────────────────────────────────────
export function setup() {
  console.log("=== Setup: 선착순 쿠폰 비동기 발급 부하테스트 ===");

  // 선착순 100장 쿠폰 생성
  const couponId = createCoupon(100);
  console.log(`쿠폰 생성: ${couponId} (선착순 100장)`);

  // 유저 500명 등록
  for (let i = 1; i <= 500; i++) {
    registerUser(`cas${String(i).padStart(4, "0")}`);
  }
  console.log("유저 500명 등록 완료");

  console.log("=== Setup 완료 ===\n");
  return { couponId, requestIds: {} };
}

// ── 시나리오 1: 비동기 발급 요청 burst ────────────────
export function couponAsyncTest(data) {
  const vuId = __VU;
  const loginId = `cas${String(vuId).padStart(4, "0")}`;

  const res = http.post(
    `${BASE_URL}/api/v1/coupons/${data.couponId}/issue-async`,
    null,
    { headers: authHeaders(loginId) },
  );

  check(res, {
    "비동기 발급 요청 accepted": (r) => r.status === 202,
  });

  if (res.status === 202) {
    issueRequested.add(1);
    const body = res.json();
    // requestId를 태그에 저장 (polling에서 사용)
    if (body.data && body.data.requestId) {
      // k6에서는 VU간 데이터 공유가 제한적이므로, polling에서 직접 재요청
    }
  }
}

// ── 시나리오 2: 발급 결과 polling ─────────────────────
export function couponPollingTest(data) {
  const vuId = __VU;
  const loginId = `cas${String(vuId).padStart(4, "0")}`;

  // 먼저 내 requestId를 다시 요청 (이미 발급 요청한 상태이므로 중복 처리될 수 있음)
  // 대안: 직접 issue-async 호출 후 requestId로 polling
  const issueRes = http.post(
    `${BASE_URL}/api/v1/coupons/${data.couponId}/issue-async`,
    null,
    { headers: authHeaders(loginId) },
  );

  if (issueRes.status !== 202) {
    return;
  }

  const requestId = issueRes.json().data.requestId;
  const start = Date.now();

  // 최대 10초간 polling (1초 간격)
  for (let attempt = 0; attempt < 10; attempt++) {
    sleep(1);

    const pollRes = http.get(
      `${BASE_URL}/api/v1/coupons/issue-requests/${requestId}`,
      { headers: authHeaders(loginId) },
    );

    if (pollRes.status === 200) {
      const pollBody = pollRes.json();
      const status = pollBody.data.status;

      if (status === "SUCCESS") {
        issueSuccess.add(1);
        pollingDuration.add(Date.now() - start);
        return;
      } else if (status === "FAILED") {
        issueFail.add(1);
        pollingDuration.add(Date.now() - start);
        return;
      }
      // PENDING → 계속 polling
    }
  }

  // 10초 내 처리 안됨
  issuePending.add(1);
  pollingDuration.add(Date.now() - start);
}

// ── teardown ──────────────────────────────────────────
export function teardown(data) {
  console.log("\n=== 선착순 쿠폰 비동기 발급 부하테스트 결과 ===");
  console.log(`쿠폰 ID: ${data.couponId} (선착순 100장, 500명 요청)`);
  console.log("커스텀 메트릭:");
  console.log("  coupon_issue_requested: 비동기 발급 요청 수");
  console.log("  coupon_issue_success: 발급 성공 수 (기대: 100)");
  console.log("  coupon_issue_fail: 발급 실패 수 (기대: 400)");
  console.log("  coupon_issue_pending: 타임아웃 (기대: 0)");
  console.log("  coupon_polling_duration: polling 소요 시간");
  console.log("================================================\n");
}
