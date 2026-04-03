import http from "k6/http";
import { check, sleep } from "k6";
import { BASE_URL, memberHeaders, adminHeaders } from "./helpers.js";

/**
 * Stress Test — 한계 탐색
 * Peak: 10,000 VUs
 */
export const options = {
    stages: [
        { duration: "1m", target: 1000 },
        { duration: "2m", target: 5000 },
        { duration: "2m", target: 10000 },
        { duration: "2m", target: 10000 },
        { duration: "1m", target: 0 },
    ],
    thresholds: {
        http_req_failed: ["rate<0.10"],
        http_req_duration: ["p(99)<5000"],
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
    const userId = `stress_user_${__VU}_${__ITER}`;
    const headers = memberHeaders(userId, "test1234!");

    const enterRes = http.post(`${BASE_URL}/api/v1/queue/enter`, null, {
        headers,
        tags: { name: "enter" },
    });
    check(enterRes, { "enter ok": (r) => r.status === 200 || r.status === 429 });

    const data = enterRes.status === 200 ? JSON.parse(enterRes.body).data : null;
    const retryAfter = data ? data.retryAfter : 10;
    sleep(retryAfter);

    const posRes = http.get(`${BASE_URL}/api/v1/queue/position`, {
        headers,
        tags: { name: "position" },
    });
    check(posRes, { "position ok": (r) => r.status === 200 || r.status === 404 });

    sleep(1);
}

export function teardown() {
    http.post(
        `${BASE_URL}/api-admin/v1/queue/toggle`,
        JSON.stringify({ enabled: false }),
        { headers: adminHeaders() },
    );
}
