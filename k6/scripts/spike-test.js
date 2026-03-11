import http from 'k6/http';
import { check } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Spike Test: 갑자기 트래픽이 몰리는 상황
// 평소 100 TPS → 순간 2000 TPS → 다시 100 TPS
export const options = {
  scenarios: {
    spike: {
      executor: 'ramping-arrival-rate',
      startRate: 100,
      timeUnit: '1s',
      preAllocatedVUs: 200,
      maxVUs: 500,
      stages: [
        { duration: '10s', target: 100 },   // 평소 트래픽
        { duration: '5s', target: 2000 },   // 급증
        { duration: '20s', target: 2000 },  // 유지
        { duration: '5s', target: 100 },    // 복귀
        { duration: '20s', target: 100 },   // 안정화
      ],
    },
  },

  thresholds: {
    http_req_duration: ['p(95)<2000'],
    errors: ['rate<0.3'],
  },
};

const headers = { 'Content-Type': 'application/json' };

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/products`, { headers });

  const success = check(res, {
    'status 200': (r) => r.status === 200,
  });
  errorRate.add(!success);
}
