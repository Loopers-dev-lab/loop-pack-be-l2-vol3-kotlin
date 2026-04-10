import http from "k6/http";
import { check, sleep } from "k6";

export const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
export const PASSWORD = "test1234!";

// --- Header builders ---

export function memberHeaders(loginId) {
    return {
        "Content-Type": "application/json",
        "X-Loopers-LoginId": loginId,
        "X-Loopers-LoginPw": PASSWORD,
    };
}

export function memberHeadersWithToken(loginId, queueToken) {
    return {
        "Content-Type": "application/json",
        "X-Loopers-LoginId": loginId,
        "X-Loopers-LoginPw": PASSWORD,
        "X-Loopers-QueueToken": queueToken,
    };
}

export function adminHeaders() {
    return {
        "Content-Type": "application/json",
        "X-Loopers-Ldap": "loopers.admin",
    };
}

// --- User registration ---

export function registerUser(loginId) {
    const res = http.post(
        `${BASE_URL}/api/v1/members`,
        JSON.stringify({
            loginId: loginId,
            password: PASSWORD,
            name: `Test${loginId}`,
            birthday: "1990-01-01",
            email: `${loginId}@test.com`,
        }),
        { headers: { "Content-Type": "application/json" }, tags: { name: "register" } },
    );
    return res.status === 201 || res.status === 409;
}

export function registerTestUsers(prefix, count) {
    for (let i = 1; i <= count; i++) {
        registerUser(`${prefix}${i}`);
    }
}

// --- Queue operations ---

export function toggleQueue(enabled) {
    const res = http.post(
        `${BASE_URL}/api-admin/v1/queue/toggle`,
        JSON.stringify({ enabled }),
        { headers: adminHeaders(), tags: { name: "toggle" } },
    );
    return res;
}

export function getQueueStatus() {
    const res = http.get(`${BASE_URL}/api-admin/v1/queue/status`, {
        headers: adminHeaders(),
        tags: { name: "adminStatus" },
    });
    return res;
}

export function enterQueue(loginId) {
    return http.post(`${BASE_URL}/api/v1/queue/enter`, null, {
        headers: memberHeaders(loginId),
        tags: { name: "enter" },
    });
}

export function getPosition(loginId) {
    return http.get(`${BASE_URL}/api/v1/queue/position`, {
        headers: memberHeaders(loginId),
        tags: { name: "position" },
    });
}

export function parseBody(res) {
    try {
        return JSON.parse(res.body);
    } catch (e) {
        return null;
    }
}

// --- Polling until token ---

export function pollUntilToken(loginId, maxAttempts) {
    const limit = maxAttempts || 20;
    for (let i = 0; i < limit; i++) {
        const res = getPosition(loginId);
        if (res.status !== 200) {
            sleep(3);
            continue;
        }
        const body = parseBody(res);
        if (body && body.data && body.data.token) {
            return body.data.token;
        }
        const retryAfter = (body && body.data) ? body.data.retryAfter : 3;
        sleep(retryAfter);
    }
    return null;
}

// --- Order with queue token ---

export function createOrder(loginId, queueToken, items) {
    const orderItems = items || [{ productId: 1, quantity: 1 }];
    return http.post(
        `${BASE_URL}/api/v1/orders`,
        JSON.stringify({ items: orderItems }),
        {
            headers: memberHeadersWithToken(loginId, queueToken),
            tags: { name: "order" },
        },
    );
}

export function createOrderWithoutToken(loginId, items) {
    const orderItems = items || [{ productId: 1, quantity: 1 }];
    return http.post(
        `${BASE_URL}/api/v1/orders`,
        JSON.stringify({ items: orderItems }),
        {
            headers: memberHeaders(loginId),
            tags: { name: "orderNoToken" },
        },
    );
}
