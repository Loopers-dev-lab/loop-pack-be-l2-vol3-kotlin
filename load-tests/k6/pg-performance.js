import http from 'k6/http';
import { check, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
export const responseTime = new Trend('pg_response_time');
export const slowCalls = new Counter('pg_slow_calls'); // 500ms 초과
export const fastCalls = new Counter('pg_fast_calls');  // 500ms 이하
export const errorRate = new Rate('pg_errors');

const PG_BASE_URL = __ENV.PG_URL || 'http://localhost:8082';

export const options = {
  scenarios: {
    normal_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 10 },   // 10초에 10명으로 증가
        { duration: '30s', target: 50 },   // 30초에 50명으로 증가
        { duration: '20s', target: 10 },   // 20초에 10명으로 감소
        { duration: '10s', target: 0 },    // 마무리
      ],
      gracefulRampDown: '5s',
    },
  },
  thresholds: {
    'pg_response_time': ['p(95)<1000', 'p(99)<1500', 'avg<600'],
    'pg_errors': ['rate<0.05'],
  },
};

export default function () {
  const orderId = 'ORDER' + Math.floor(Math.random() * 1000000);
  const amount = Math.floor(Math.random() * 50000 + 1000);

  const payload = JSON.stringify({
    orderId: orderId,
    cardType: 'SAMSUNG',
    cardNo: '1234-5678-9814-1451',
    amount: amount.toString(),
    callbackUrl: 'http://localhost:8080/api/v1/payments/callback',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-USER-ID': Math.floor(Math.random() * 1000) + 1,
    },
    timeout: '15s',
  };

  group('PG Payment Request', () => {
    const startTime = Date.now();
    const res = http.post(`${PG_BASE_URL}/api/v1/payments`, payload, params);
    const duration = Date.now() - startTime;

    // 메트릭 기록
    responseTime.add(duration);

    if (duration >= 500) {
      slowCalls.add(1);
    } else {
      fastCalls.add(1);
    }

    // 응답 확인
    const isSuccess = check(res, {
      'PG status 2xx': (r) => r.status >= 200 && r.status < 300,
      'response time < 1s': (r) => r.timings.duration < 1000,
      'response time < 500ms': (r) => r.timings.duration < 500,
    });

    if (!isSuccess) {
      errorRate.add(1);
    }

    if (res.status !== 200) {
      console.log(`⚠️ PG Error - Status: ${res.status}, Duration: ${duration}ms, Order: ${orderId}`);
      console.log(`Response: ${res.body}`);
    }
  });
}

export function teardown(data) {
  console.log('\n=== PG Performance Test Summary ===');
  console.log(`Slow calls (≥500ms): ${slowCalls.value}`);
  console.log(`Fast calls (<500ms): ${fastCalls.value}`);
}
