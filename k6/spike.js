import http from "k6/http";
import { check, sleep } from "k6";
import { BASE_URL, memberHeaders, adminHeaders } from "./helpers.js";

/**
 * Spike Test — Thundering Herd 시나리오
 * 0 → 10,000 VUs 순간 급증
 */
export const options = {
    stages: [
        { duration: "10s", target: 0 },
        { duration: "5s", target: 10000 },   // 5초 만에 10,000 동시 접속
        { duration: "2m", target: 10000 },   // 유지
        { duration: "30s", target: 0 },      // 종료
    ],
    thresholds: {
        http_req_failed: ["rate<0.15"],
        http_req_duration: ["p(95)<10000"],
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
    const userId = `spike_user_${__VU}_${__ITER}`;
    const headers = memberHeaders(userId, "test1234!");

    // 대기열 진입 — 동시 급증 핵심
    const enterRes = http.post(`${BASE_URL}/api/v1/queue/enter`, null, {
        headers,
        tags: { name: "enter" },
    });

    const ok = enterRes.status === 200;
    check(enterRes, { "enter handled": () => ok || enterRes.status === 429 || enterRes.status === 503 });

    if (ok) {
        const data = JSON.parse(enterRes.body).data;
        const retryAfter = data ? data.retryAfter : 10;
        sleep(retryAfter);

        const posRes = http.get(`${BASE_URL}/api/v1/queue/position`, {
            headers,
            tags: { name: "position" },
        });
        check(posRes, { "position handled": (r) => r.status === 200 || r.status === 404 });
    }

    sleep(1);
}

export function teardown() {
    http.post(
        `${BASE_URL}/api-admin/v1/queue/toggle`,
        JSON.stringify({ enabled: false }),
        { headers: adminHeaders() },
    );
}
