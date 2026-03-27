import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Trend } from "k6/metrics";

// ── 설정 ──────────────────────────────────────────────
const COMMERCE_URL = __ENV.COMMERCE_URL || "http://host.docker.internal:8080";
const PG_URL = __ENV.PG_URL || "http://host.docker.internal:8082";
const PASSWORD = "Password1!";

// 커스텀 메트릭
const paymentSuccess = new Counter("payment_success");
const paymentFail = new Counter("payment_fail");
const paymentPending = new Counter("payment_pending");
const pgRequestDuration = new Trend("pg_request_duration", true);

// ── 시나리오 ──────────────────────────────────────────
export const options = {
    scenarios: {
        // Phase 1: 결제 요청 부하 (ramp up → sustain → ramp down)
        payment_load: {
            executor: "ramping-vus",
            stages: [
                { duration: "10s", target: 5 },
                { duration: "30s", target: 20 },
                { duration: "20s", target: 20 },
                { duration: "10s", target: 0 },
            ],
            exec: "paymentTest",
            startTime: "0s",
            tags: { scenario: "payment" },
        },
        // Phase 2: PG 시뮬레이터 성능 측정 (직접 호출)
        pg_benchmark: {
            executor: "constant-vus",
            vus: 10,
            duration: "20s",
            exec: "pgBenchmark",
            startTime: "75s",
            tags: { scenario: "pg_benchmark" },
        },
    },
    thresholds: {
        http_req_duration: ["p(95)<5000"],
        "http_req_duration{scenario:pg_benchmark}": ["p(95)<3000"],
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
        `${COMMERCE_URL}/api/v1/users`,
        JSON.stringify({
            loginId: loginId,
            password: PASSWORD,
            name: "결제테스트",
            birthDate: "1993-04-01",
            email: `${loginId}@test.com`,
            gender: "MALE",
        }),
        { headers: { "Content-Type": "application/json" } },
    );
}

function createBrand() {
    const res = http.post(
        `${COMMERCE_URL}/api-admin/v1/brands`,
        JSON.stringify({
            name: `결제테스트브랜드_${Date.now()}`,
            description: null,
            logoUrl: null,
        }),
        { headers: adminHeaders() },
    );
    return JSON.parse(res.body).data.id;
}

function createProduct(brandId, stock) {
    const res = http.post(
        `${COMMERCE_URL}/api-admin/v1/products`,
        JSON.stringify({
            brandId: brandId,
            name: `결제테스트상품_${Date.now()}`,
            description: "결제 부하테스트용",
            price: 10000,
            stock: stock,
            thumbnailUrl: null,
            images: [],
        }),
        { headers: adminHeaders() },
    );
    return JSON.parse(res.body).data.id;
}

function createOrder(loginId, productId) {
    const res = http.post(
        `${COMMERCE_URL}/api/v1/orders`,
        JSON.stringify({
            items: [{ productId: productId, quantity: 1 }],
        }),
        { headers: authHeaders(loginId) },
    );
    if (res.status === 201) {
        return JSON.parse(res.body).data.id;
    }
    return null;
}

// ── setup: 테스트 데이터 준비 ─────────────────────────
export function setup() {
    console.log("=== Setup: 결제 부하테스트 데이터 준비 ===");

    const brandId = createBrand();
    console.log(`브랜드 생성: ${brandId}`);

    // 재고 넉넉하게 — 결제 테스트이므로 재고 부족은 테스트 대상 아님
    const productId = createProduct(brandId, 10000);
    console.log(`상품 생성: ${productId} (재고: 10000)`);

    // 유저 50명 등록 + 주문 미리 생성
    const users = [];
    for (let i = 1; i <= 50; i++) {
        const loginId = `pay${String(i).padStart(3, "0")}`;
        registerUser(loginId);
        const orderId = createOrder(loginId, productId);
        if (orderId) {
            users.push({ loginId, orderId });
        }
    }
    console.log(`유저 + 주문 ${users.length}건 준비 완료`);

    // PG 벤치마크용 유저
    for (let i = 51; i <= 60; i++) {
        registerUser(`pgb${String(i).padStart(3, "0")}`);
    }

    console.log("=== Setup 완료 ===\n");
    return { productId, brandId, users };
}

// ── 시나리오 1: 결제 요청 부하 ────────────────────────
export function paymentTest(data) {
    const idx = (__VU - 1) % data.users.length;
    const user = data.users[idx];

    // 매 iteration마다 새 주문 생성 (한 주문에 한 결제)
    const orderId = createOrder(user.loginId, data.productId);
    if (!orderId) {
        paymentFail.add(1);
        sleep(0.5);
        return;
    }

    const res = http.post(
        `${COMMERCE_URL}/api/v1/payments`,
        JSON.stringify({
            orderId: orderId,
            cardType: "SAMSUNG",
            cardNo: "1234-5678-9012-3456",
        }),
        { headers: authHeaders(user.loginId) },
    );

    const body = res.json();

    if (res.status === 201) {
        paymentSuccess.add(1);

        // 결제 상태 확인 (폴링)
        if (body.data && body.data.paymentId) {
            sleep(2); // PG 비동기 처리 대기
            const statusRes = http.get(
                `${COMMERCE_URL}/api/v1/payments/${body.data.paymentId}`,
                { headers: authHeaders(user.loginId) },
            );
            const statusBody = statusRes.json();
            if (statusBody.data && statusBody.data.status === "PENDING") {
                paymentPending.add(1);
            }
        }
    } else {
        paymentFail.add(1);
    }

    sleep(0.5);
}

// ── 시나리오 2: PG 시뮬레이터 직접 벤치마크 ──────────
export function pgBenchmark() {
    const vuId = 50 + (__VU % 10) + 1;
    const orderId = `bench_${Date.now()}_${__VU}_${__ITER}`;

    const start = Date.now();
    const res = http.post(
        `${PG_URL}/api/v1/payments`,
        JSON.stringify({
            orderId: orderId,
            cardType: "SAMSUNG",
            cardNo: "1234-5678-9012-3456",
            amount: 10000,
            callbackUrl: `${COMMERCE_URL}/api/v1/payments/callback`,
        }),
        {
            headers: {
                "Content-Type": "application/json",
                "X-USER-ID": String(vuId),
            },
        },
    );
    pgRequestDuration.add(Date.now() - start);

    check(res, {
        "PG 응답 수신": (r) => r.status === 200 || r.status === 400 || r.status === 500,
    });

    if (res.status === 200) {
        const body = res.json();
        if (body.data && body.data.transactionKey) {
            paymentSuccess.add(1);
        } else {
            paymentFail.add(1);
        }
    } else {
        paymentFail.add(1);
    }

    sleep(0.2);
}

// ── teardown ──────────────────────────────────────────
export function teardown(data) {
    console.log("\n=== 결제 부하테스트 결과 요약 ===");
    console.log("커스텀 메트릭에서 payment_success / payment_fail / payment_pending 확인");
    console.log("pg_request_duration에서 PG 시뮬레이터 응답 시간 분포 확인");
    console.log("================================\n");
}
