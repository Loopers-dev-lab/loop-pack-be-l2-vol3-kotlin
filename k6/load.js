import http from "k6/http";
import { check, sleep } from "k6";
import { BASE_URL, memberHeaders, adminHeaders } from "./helpers.js";

/**
 * Load Test — 정상 트래픽 처리량 검증
 * Peak: 1,000 VUs, Duration: 5m
 */
export const options = {
    stages: [
        { duration: "30s", target: 200 },
        { duration: "1m", target: 500 },
        { duration: "2m", target: 1000 },
        { duration: "1m", target: 500 },
        { duration: "30s", target: 0 },
    ],
    thresholds: {
        http_req_failed: ["rate<0.05"],
        http_req_duration: ["p(95)<2000"],
        "http_req_duration{name:enter}": ["p(99)<3000"],
        "http_req_duration{name:position}": ["p(99)<1000"],
    },
};

export function setup() {
    const res = http.post(
        `${BASE_URL}/api-admin/v1/queue/toggle`,
        JSON.stringify({ enabled: true }),
        { headers: adminHeaders() },
    );
    check(res, { "toggle 200": (r) => r.status === 200 });
}

export default function () {
    const userId = `load_user_${__VU}_${__ITER}`;
    const headers = memberHeaders(userId, "test1234!");

    // 대기열 진입
    const enterRes = http.post(`${BASE_URL}/api/v1/queue/enter`, null, {
        headers,
        tags: { name: "enter" },
    });
    check(enterRes, { "enter ok": (r) => r.status === 200 });

    // Polling 시뮬레이션
    const data = JSON.parse(enterRes.body).data;
    const retryAfter = data ? data.retryAfter : 3;
    sleep(retryAfter);

    const posRes = http.get(`${BASE_URL}/api/v1/queue/position`, {
        headers,
        tags: { name: "position" },
    });
    check(posRes, { "position ok": (r) => r.status === 200 });

    sleep(1);
}

export function teardown() {
    http.post(
        `${BASE_URL}/api-admin/v1/queue/toggle`,
        JSON.stringify({ enabled: false }),
        { headers: adminHeaders() },
    );
}
