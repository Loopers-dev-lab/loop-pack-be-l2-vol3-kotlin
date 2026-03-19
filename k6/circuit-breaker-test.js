import http from "k6/http";
import { check, sleep } from "k6";
import { Counter } from "k6/metrics";

/**
 * 서킷브레이커 상태 전이 테스트
 *
 * 사전 조건:
 *   - commerce-api 기동 (port 8080)
 *   - pg-simulator 꺼진 상태 (결제 요청 전부 실패하도록)
 *
 * 검증 항목:
 *   1. PG 장애 시 결제 요청이 fallback으로 처리되는지
 *   2. 실패율 70% 초과 시 서킷이 OPEN되는지
 *   3. OPEN 상태에서 요청이 즉시 거부되는지 (thread 점유 없음)
 *   4. 10초 후 HALF_OPEN 전이 → 시험 요청 → 다시 OPEN
 *
 * 실행:
 *   k6 run circuit-breaker-test.js
 *
 * Grafana 확인:
 *   Resilience4j 대시보드 (ID: 21307) import 후 모니터링
 */

const COMMERCE_URL = __ENV.COMMERCE_URL || "http://host.docker.internal:8080";
const PASSWORD = "Password1!";

const cbOpen = new Counter("cb_open_rejected");
const cbClosed = new Counter("cb_closed_attempted");

export const options = {
    scenarios: {
        // 느린 속도로 결제 요청 → 서킷 전이 관찰
        circuit_breaker_trigger: {
            executor: "constant-arrival-rate",
            rate: 2,
            timeUnit: "1s",
            duration: "60s",
            preAllocatedVUs: 5,
            exec: "triggerCircuitBreaker",
        },
    },
    thresholds: {
        http_req_duration: ["p(95)<5000"],
    },
};

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
            name: "CB테스트",
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
        JSON.stringify({ name: `CB테스트브랜드_${Date.now()}`, description: null, logoUrl: null }),
        { headers: adminHeaders() },
    );
    return JSON.parse(res.body).data.id;
}

function createProduct(brandId) {
    const res = http.post(
        `${COMMERCE_URL}/api-admin/v1/products`,
        JSON.stringify({
            brandId: brandId,
            name: `CB상품_${Date.now()}`,
            description: "서킷브레이커 테스트",
            price: 10000,
            stock: 50000,
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
        JSON.stringify({ items: [{ productId: productId, quantity: 1 }] }),
        { headers: authHeaders(loginId) },
    );
    if (res.status === 201) {
        return JSON.parse(res.body).data.id;
    }
    return null;
}

export function setup() {
    console.log("=== 서킷브레이커 테스트 Setup ===");
    console.log("⚠️  pg-simulator가 꺼진 상태에서 실행해야 합니다!");
    console.log("");

    const brandId = createBrand();
    const productId = createProduct(brandId);

    const users = [];
    for (let i = 1; i <= 5; i++) {
        const loginId = `cbt${String(i).padStart(3, "0")}`;
        registerUser(loginId);
        users.push(loginId);
    }

    console.log(`유저 ${users.length}명 준비 완료`);
    console.log("=== Setup 완료 ===\n");

    return { productId, users };
}

export function triggerCircuitBreaker(data) {
    const idx = (__VU - 1) % data.users.length;
    const loginId = data.users[idx];

    // 매번 새 주문 생성
    const orderId = createOrder(loginId, data.productId);
    if (!orderId) {
        sleep(0.5);
        return;
    }

    const start = Date.now();
    const res = http.post(
        `${COMMERCE_URL}/api/v1/payments`,
        JSON.stringify({
            orderId: orderId,
            cardType: "SAMSUNG",
            cardNo: "1234-5678-9012-3456",
        }),
        { headers: authHeaders(loginId) },
    );
    const elapsed = Date.now() - start;

    // 서킷 OPEN 여부 판단: 응답이 매우 빠르면(< 100ms) 서킷이 열려서 즉시 거부된 것
    if (elapsed < 100 && res.status !== 201) {
        cbOpen.add(1);
        console.log(`[${new Date().toISOString()}] CB OPEN — 즉시 거부 (${elapsed}ms)`);
    } else {
        cbClosed.add(1);
        console.log(`[${new Date().toISOString()}] CB CLOSED/HALF_OPEN — PG 호출 시도 (${elapsed}ms, status=${res.status})`);
    }

    // actuator로 서킷 상태 확인
    if (__ITER % 5 === 0) {
        const actuator = http.get(`${COMMERCE_URL}/actuator/health`, {
            headers: { "Content-Type": "application/json" },
        });
        if (actuator.status === 200) {
            console.log(`[Actuator] ${actuator.body.substring(0, 200)}`);
        }
    }
}

export function teardown() {
    console.log("\n=== 서킷브레이커 테스트 결과 ===");
    console.log("cb_open_rejected: 서킷 OPEN 상태에서 즉시 거부된 요청 수");
    console.log("cb_closed_attempted: 서킷 CLOSED/HALF_OPEN 상태에서 PG 호출을 시도한 요청 수");
    console.log("");
    console.log("Grafana에서 확인할 패널:");
    console.log("  - Resilience4j 대시보드 (ID: 21307) import");
    console.log("  - Circuit Breaker State (CLOSED → OPEN → HALF_OPEN)");
    console.log("  - Failure Rate");
    console.log("  - Not Permitted Calls");
    console.log("================================\n");
}
