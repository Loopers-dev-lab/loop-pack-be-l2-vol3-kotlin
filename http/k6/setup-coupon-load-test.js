/**
 * k6 사전 준비 스크립트 — 선착순 쿠폰 부하테스트용 유저 + 쿠폰 생성
 *
 * 실행: k6 run http/k6/setup-coupon-load-test.js
 *
 * 이 스크립트가 하는 일:
 * 1. 100명의 유저 회원가입 (user1 ~ user100)
 * 2. 선착순 쿠폰 1개 생성 (maxIssueCount=50, 50장 한정)
 *
 * 이후 coupon-issue-load-test.js를 실행하면 100명이 동시에 50장 쿠폰을 요청합니다.
 */
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'http://localhost:8080';
const ADMIN_HEADER = { 'X-Loopers-Ldap': 'loopers.admin' };
const TOTAL_USERS = 100;

export const options = {
    vus: 1,
    iterations: 1,
};

export default function () {
    // 1. 유저 100명 회원가입
    for (let i = 1; i <= TOTAL_USERS; i++) {
        const res = http.post(
            `${BASE_URL}/api/v1/users`,
            JSON.stringify({
                loginId: `loadtest${i}`,
                password: 'Test1234!',
                name: `부하테스트유저${i}`,
                email: `loadtest${i}@test.com`,
                birthday: '1990-01-01',
            }),
            { headers: { 'Content-Type': 'application/json' } },
        );
        if (i % 10 === 0) {
            console.log(`유저 생성 ${i}/${TOTAL_USERS}`);
        }
    }

    // 2. 선착순 쿠폰 생성 (50장 한정)
    const couponRes = http.post(
        `${BASE_URL}/api-admin/v1/coupons`,
        JSON.stringify({
            name: 'k6 선착순 쿠폰',
            type: 'FIXED',
            value: 5000,
            expiredAt: '2026-12-31T23:59:59+09:00',
            maxIssueCount: 50,
        }),
        {
            headers: {
                'Content-Type': 'application/json',
                ...ADMIN_HEADER,
            },
        },
    );

    const body = JSON.parse(couponRes.body);
    const couponId = body.data?.id;
    console.log(`\n쿠폰 생성 완료 — couponId: ${couponId}`);
    console.log(`\n다음 명령어로 부하테스트 실행:`);
    console.log(`k6 run -e COUPON_ID=${couponId} http/k6/coupon-issue-load-test.js`);
}
