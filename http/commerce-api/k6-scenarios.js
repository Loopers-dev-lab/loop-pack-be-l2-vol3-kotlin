import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const paymentPending = new Counter('payment_pending');
const paymentFallback = new Counter('payment_fallback');
const successRate = new Rate('payment_success_rate');
const responseTime = new Trend('payment_response_time');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const LOGIN_ID = __ENV.LOGIN_ID || 'loadtest001';
const LOGIN_PW = __ENV.LOGIN_PW || 'Test1234!@';
const SCENARIO = __ENV.SCENARIO || 'default';

// 시나리오별 설정
const scenarioConfigs = {
  // 시나리오 1: PG 40% 실패 (기본)
  'pg-unstable': {
    stages: [
      { duration: '5s', target: 10 },
      { duration: '20s', target: 30 },
      { duration: '10s', target: 30 },
      { duration: '5s', target: 0 },
    ],
  },
  // 시나리오 2: PG 완전 장애 (시뮬레이터 중지 상태)
  'pg-down': {
    stages: [
      { duration: '5s', target: 10 },
      { duration: '15s', target: 30 },
      { duration: '5s', target: 0 },
    ],
  },
  // 시나리오 3: PG 장애 → 복구 (시뮬레이터 중지 후 수동 재시작)
  'pg-recovery': {
    stages: [
      { duration: '10s', target: 20 },
      { duration: '30s', target: 20 },
      { duration: '20s', target: 20 },
      { duration: '5s', target: 0 },
    ],
  },
  // 시나리오 4: 동시 대량 요청 (버스트)
  'burst': {
    stages: [
      { duration: '3s', target: 50 },
      { duration: '10s', target: 50 },
      { duration: '3s', target: 0 },
    ],
  },
};

const config = scenarioConfigs[SCENARIO] || scenarioConfigs['pg-unstable'];

export const options = {
  scenarios: {
    load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: config.stages,
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<5000'],
  },
};

export function setup() {
  // 이미 생성된 사용자 사용 (409 무시)
  http.post(
    `${BASE_URL}/api/v1/users/signup`,
    JSON.stringify({
      loginId: LOGIN_ID,
      password: LOGIN_PW,
      name: '부하테스트',
      email: 'loadtest@example.com',
      birthday: '1990-01-15',
    }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  return { loginId: LOGIN_ID, password: LOGIN_PW };
}

export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    'X-Loopers-LoginId': data.loginId,
    'X-Loopers-LoginPw': data.password,
  };

  const orderId = `ORDER-${__VU}-${__ITER}-${Date.now()}`;
  const payload = JSON.stringify({
    orderId: orderId,
    cardType: 'SAMSUNG',
    cardNo: '1234-5678-9012-3456',
    amount: 50000,
  });

  const res = http.post(`${BASE_URL}/api/v1/payments`, payload, { headers });
  responseTime.add(res.timings.duration);

  const isOk = check(res, { '응답 200': (r) => r.status === 200 });
  successRate.add(isOk);

  if (res.status === 200) {
    try {
      const body = JSON.parse(res.body);
      const status = body.data?.status;
      if (status === 'PENDING') paymentPending.add(1);
      else if (status === 'REQUESTED') paymentFallback.add(1);
    } catch (e) {}
  }

  sleep(0.05 + Math.random() * 0.1);
}
