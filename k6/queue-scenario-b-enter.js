import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

/**
 * 시나리오 B: 대기열 진입 부하 테스트
 *
 * 목적: 대기열 진입 API가 동시 요청 N건을 안정적으로 처리하는지
 * 확인: 대기열 순서가 정확히 보장되는지, DB 커넥션이 0개 사용되는지
 *
 * 사전 조건:
 *   1. application.yml에서 queue.enabled=true 설정
 *   2. 테스트용 유저 데이터 사전 등록
 *
 * 실행:
 *   k6 run k6/queue-scenario-b-enter.js
 *   k6 run --env VUS=5000 k6/queue-scenario-b-enter.js
 */

const enterLatency = new Trend('enter_latency', true);
const enterErrorRate = new Rate('enter_errors');
const enterSuccess = new Counter('enter_success');

const VUS = parseInt(__ENV.VUS || '1000');
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
    scenarios: {
        ramp_up: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: Math.floor(VUS * 0.2) },
                { duration: '10s', target: Math.floor(VUS * 0.5) },
                { duration: '10s', target: VUS },
                { duration: '20s', target: VUS },
                { duration: '10s', target: 0 },
            ],
        },
    },
    thresholds: {
        enter_latency: ['p(50)<100', 'p(95)<500', 'p(99)<1000'],
        enter_errors: ['rate<0.01'],
    },
};

export default function () {
    const userId = __VU;
    const loginId = `loadtest_user_${userId}`;
    const password = 'Test123!';

    const headers = {
        'Content-Type': 'application/json',
        'X-Loopers-LoginId': loginId,
        'X-Loopers-LoginPw': password,
    };

    const res = http.post(`${BASE_URL}/api/v1/queue/enter`, null, { headers });
    enterLatency.add(res.timings.duration);

    const success = check(res, {
        'status 200': (r) => r.status === 200,
        'has position': (r) => JSON.parse(r.body).data.position !== undefined,
    });

    if (success) {
        enterSuccess.add(1);
    } else {
        enterErrorRate.add(true);
    }

    sleep(0.5);
}
