import http from "k6/http";
import { check, sleep } from "k6";
import { BASE_URL, memberHeaders, adminHeaders } from "./helpers.js";

/**
 * Smoke Test — 기능 정상 동작 확인
 * VUs: 10, Duration: 30s
 */
export const options = {
    vus: 10,
    duration: "30s",
    thresholds: {
        http_req_failed: ["rate<0.01"],
        http_req_duration: ["p(95)<1000"],
    },
};

export function setup() {
    // 대기열 활성화
    const res = http.post(
        `${BASE_URL}/api-admin/v1/queue/toggle`,
        JSON.stringify({ enabled: true }),
        { headers: adminHeaders() },
    );
    check(res, { "toggle 200": (r) => r.status === 200 });
}

export default function () {
    const vu = __VU;
    const loginId = `loadtest_user_${vu}`;
    const password = "test1234!";
    const headers = memberHeaders(loginId, password);

    // 1. 대기열 진입
    const enterRes = http.post(`${BASE_URL}/api/v1/queue/enter`, null, { headers });
    check(enterRes, {
        "enter 200": (r) => r.status === 200,
        "has position": (r) => JSON.parse(r.body).data.position >= 0,
    });

    sleep(1);

    // 2. 순번 조회
    const posRes = http.get(`${BASE_URL}/api/v1/queue/position`, { headers });
    check(posRes, {
        "position 200": (r) => r.status === 200,
        "has retryAfter": (r) => JSON.parse(r.body).data.retryAfter >= 0,
    });

    sleep(1);
}

export function teardown() {
    // 대기열 비활성화
    http.post(
        `${BASE_URL}/api-admin/v1/queue/toggle`,
        JSON.stringify({ enabled: false }),
        { headers: adminHeaders() },
    );
}
