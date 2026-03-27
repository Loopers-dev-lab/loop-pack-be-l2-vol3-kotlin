/**
 * k6 선착순 쿠폰 발급 부하테스트
 *
 * 사전 준비: setup-coupon-load-test.js 실행 후 couponId 확인
 * 실행: k6 run -e COUPON_ID={쿠폰ID} http/k6/coupon-issue-load-test.js
 *
 * 시나리오:
 * - 100명의 유저가 동시에 50장 한정 쿠폰을 요청
 * - commerce-api가 Kafka에 발행 → commerce-streamer가 순차 처리
 * - Kafka UI(localhost:9099)에서 coupon-issue-requests 토픽 실시간 확인
 *
 * 확인 포인트:
 * 1. Kafka UI: coupon-issue-requests 토픽에 100건 메시지
 * 2. DB: coupon_issue_request 테이블 — SUCCESS 50건, EXHAUSTED 50건
 * 3. DB: coupon_issues 테이블 — 정확히 50건
 * 4. DB: coupons.issued_count = 50
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = 'http://localhost:8080';
const COUPON_ID = __ENV.COUPON_ID;
const TOTAL_USERS = 100;

// 커스텀 메트릭
const successRequests = new Counter('success_requests');
const conflictRequests = new Counter('conflict_requests');
const errorRequests = new Counter('error_requests');

export const options = {
    // 100명 동시 요청
    scenarios: {
        coupon_rush: {
            executor: 'per-vu-iterations',
            vus: TOTAL_USERS,
            iterations: 1,
            maxDuration: '30s',
        },
    },
};

export default function () {
    const userId = __VU; // VU 번호 = 유저 번호 (1~100)
    const loginId = `loadtest${userId}`;
    const password = 'Test1234!';

    // 선착순 쿠폰 발급 요청
    const res = http.post(
        `${BASE_URL}/api/v1/coupons/${COUPON_ID}/issue-request`,
        null,
        {
            headers: {
                'X-Loopers-LoginId': loginId,
                'X-Loopers-LoginPw': password,
            },
        },
    );

    const status = res.status;
    const body = JSON.parse(res.body);

    if (status === 200) {
        successRequests.add(1);
        const requestId = body.data?.requestId;
        console.log(`[VU${userId}] 요청 접수 — requestId: ${requestId}`);

        // 2초 후 결과 폴링
        sleep(2);
        const pollRes = http.get(
            `${BASE_URL}/api/v1/coupons/issue-request/${requestId}`,
            {
                headers: {
                    'X-Loopers-LoginId': loginId,
                    'X-Loopers-LoginPw': password,
                },
            },
        );
        const pollBody = JSON.parse(pollRes.body);
        console.log(`[VU${userId}] 결과: ${pollBody.data?.status}`);
    } else if (status === 409) {
        conflictRequests.add(1);
        console.log(`[VU${userId}] 중복 요청`);
    } else {
        errorRequests.add(1);
        console.log(`[VU${userId}] 에러: ${status} - ${res.body}`);
    }
}

export function handleSummary(data) {
    const success = data.metrics.success_requests?.values?.count || 0;
    const conflict = data.metrics.conflict_requests?.values?.count || 0;
    const error = data.metrics.error_requests?.values?.count || 0;

    console.log('\n========== 결과 요약 ==========');
    console.log(`요청 접수: ${success}건`);
    console.log(`중복 요청: ${conflict}건`);
    console.log(`에러:      ${error}건`);
    console.log('================================');
    console.log('\n확인할 곳:');
    console.log('1. Kafka UI: http://localhost:9099 → coupon-issue-requests 토픽');
    console.log('2. DB: SELECT status, COUNT(*) FROM coupon_issue_request GROUP BY status');
    console.log('3. DB: SELECT COUNT(*) FROM coupon_issues');
    console.log('4. DB: SELECT issued_count FROM coupons WHERE id = ' + COUPON_ID);

    return {};
}
