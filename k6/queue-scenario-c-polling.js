import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

/**
 * 시나리오 C: Polling 부하 테스트
 *
 * 목적: 순번 조회 API가 대량 Polling에 견디는지
 * 확인: DB 커넥션 사용량이 0인지
 *
 * 사전 조건:
 *   1. 시나리오 B를 먼저 실행하여 대기열에 유저를 등록
 *   2. 또는 사전에 Redis ZADD로 유저 등록
 *
 * 실행:
 *   k6 run k6/queue-scenario-c-polling.js
 *   k6 run --env VUS=5000 k6/queue-scenario-c-polling.js
 */

const pollingLatency = new Trend('polling_latency', true);
const pollingErrorRate = new Rate('polling_errors');

const VUS = parseInt(__ENV.VUS || '5000');
const POLL_INTERVAL_SEC = parseFloat(__ENV.POLL_INTERVAL || '2');
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
    scenarios: {
        polling: {
            executor: 'constant-vus',
            vus: VUS,
            duration: '60s',
        },
    },
    thresholds: {
        polling_latency: ['p(50)<50', 'p(95)<200', 'p(99)<500'],
        polling_errors: ['rate<0.01'],
    },
};

export default function () {
    const userId = __VU;
    const loginId = `loaduser${userId}`;
    const password = 'Test1234!';

    const headers = {
        'X-Loopers-LoginId': loginId,
        'X-Loopers-LoginPw': password,
    };

    const res = http.get(`${BASE_URL}/api/v1/queue/position`, { headers });
    pollingLatency.add(res.timings.duration);

    const success = check(res, {
        'status 200': (r) => r.status === 200,
        'has status field': (r) => {
            const body = JSON.parse(r.body);
            return body.data && body.data.status !== undefined;
        },
    });

    if (!success) {
        pollingErrorRate.add(true);
    }

    sleep(POLL_INTERVAL_SEC);
}
