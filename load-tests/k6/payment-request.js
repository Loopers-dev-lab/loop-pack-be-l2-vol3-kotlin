import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
export const errorRate = new Rate('errors');
export const responseTime = new Trend('response_time');
export const slowCalls = new Counter('slow_calls'); // 500ms 초과
export const fastCalls = new Counter('fast_calls');  // 500ms 이하

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TEST_TYPE = __ENV.TEST_TYPE || 'normal'; // normal, stress, sustained

let options = {};

if (TEST_TYPE === 'normal') {
  // 시나리오 1: 정상 트래픽 (평상시) - 기본값
  options = {
    scenarios: {
      normal: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
          { duration: '10s', target: 10 },  // 10초에 10명으로 증가
          { duration: '30s', target: 50 },  // 30초에 50명으로 증가
          { duration: '20s', target: 10 },  // 20초에 10명으로 감소
          { duration: '10s', target: 0 },   // 마무리
        ],
        gracefulRampDown: '5s',
      },
    },
    thresholds: {
      'http_req_duration': ['p(95)<1000', 'p(99)<1500'],
      'http_req_failed': ['rate<0.1'],
    },
  };
} else if (TEST_TYPE === 'stress') {
  // 시나리오 2: 스트레스 테스트 (높은 동시성)
  options = {
    scenarios: {
      stress: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
          { duration: '5s', target: 50 },
          { duration: '30s', target: 100 },
          { duration: '5s', target: 0 },
        ],
      },
    },
    thresholds: {
      'http_req_duration': ['p(95)<1500'],
      'http_req_failed': ['rate<0.05'],
    },
  };
} else if (TEST_TYPE === 'sustained') {
  // 시나리오 3: 지속적인 부하 테스트
  options = {
    scenarios: {
      sustained: {
        executor: 'constant-vus',
        vus: 30,
        duration: '60s',
      },
    },
    thresholds: {
      'http_req_duration': ['p(95)<1200', 'p(99)<1500'],
      'http_req_failed': ['rate<0.1'],
    },
  };
}

export { options };

export default function () {
  const userId = Math.floor(Math.random() * 1000) + 1;
  const orderId = Math.floor(Math.random() * 100000) + 1;
  const amount = Math.random() * 50000 + 1000; // 1000 ~ 51000

  group('Payment Request Flow', () => {
    const payload = JSON.stringify({
      orderId: orderId,
      cardType: 'SAMSUNG',
      cardNo: '1234-5678-9814-1451',
      amount: amount.toString(),
    });

    const params = {
      headers: {
        'Content-Type': 'application/json',
        'X-USER-ID': userId.toString(),
      },
      timeout: '15s',
    };

    const startTime = Date.now();
    const res = http.post(`${BASE_URL}/api/v1/payments`, payload, params);
    const duration = Date.now() - startTime;

    // 응답 시간 기록
    responseTime.add(duration);

    // 500ms 이상/이하 분류
    if (duration >= 500) {
      slowCalls.add(1);
    } else {
      fastCalls.add(1);
    }

    // 체크
    const isSuccess = check(res, {
      'status is 2xx': (r) => r.status >= 200 && r.status < 300,
      'response time < 1s': (r) => r.timings.duration < 1000,
      'response time < 500ms (target)': (r) => r.timings.duration < 500,
    });

    if (!isSuccess) {
      errorRate.add(1);
    }

    console.log(`Payment Request - Status: ${res.status}, Duration: ${duration}ms, User: ${userId}, Order: ${orderId}`);
  });

  // 요청 간 대기 (realistic 시뮬레이션)
  sleep(Math.random() * 2 + 0.5); // 0.5 ~ 2.5초 대기
}

// Summary 함수
export function teardown(data) {
  console.log('=== Test Summary ===');
  console.log(`Total slow calls (≥500ms): ${slowCalls.value}`);
  console.log(`Total fast calls (<500ms): ${fastCalls.value}`);
}
