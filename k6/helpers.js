export const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

export function memberHeaders(loginId, password) {
    return {
        "Content-Type": "application/json",
        "X-Loopers-LoginId": loginId,
        "X-Loopers-LoginPw": password,
    };
}

export function memberHeadersWithToken(loginId, password, queueToken) {
    return {
        "Content-Type": "application/json",
        "X-Loopers-LoginId": loginId,
        "X-Loopers-LoginPw": password,
        "X-Loopers-QueueToken": queueToken,
    };
}

export function adminHeaders() {
    return {
        "Content-Type": "application/json",
        "X-Loopers-Ldap": "loopers.admin",
    };
}
