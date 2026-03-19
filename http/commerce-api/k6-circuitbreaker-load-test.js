import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭
const paymentRequested = new Counter('payment_requested');
const paymentPending = new Counter('payment_pending');
const paymentFallback = new Counter('payment_fallback');
const successRate = new Rate('payment_success_rate');
const responseTime = new Trend('payment_response_time');

// 테스트 시나리오 설정
export const options = {
  scenarios: {
    // 시나리오 1: 점진적 부하 증가 (서킷브레이커 CLOSED → OPEN 전이 관찰)
    ramp_up: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 10 },  // 워밍업
        { duration: '30s', target: 30 },  // 부하 증가 (PG 40% 실패 → 서킷 OPEN 예상)
        { duration: '20s', target: 30 },  // 부하 유지 (OPEN 상태 관찰)
        { duration: '20s', target: 10 },  // 부하 감소 (HALF_OPEN → CLOSED 복구 관찰)
        { duration: '10s', target: 0 },   // 종료
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<3000'],  // 95% 요청이 3초 이내
    http_req_failed: ['rate<0.8'],       // 전체 실패율 80% 미만 (PG 40% 실패 + 서킷 OPEN 감안)
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const LOGIN_ID = __ENV.LOGIN_ID || 'loadtest001';
const LOGIN_PW = __ENV.LOGIN_PW || 'Test1234!@';

// 테스트 전 사용자 생성 (1회)
export function setup() {
  const signUpRes = http.post(
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

  console.log(`회원가입 응답: ${signUpRes.status}`);
  return { loginId: LOGIN_ID, password: LOGIN_PW };
}

export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    'X-Loopers-LoginId': data.loginId,
    'X-Loopers-LoginPw': data.password,
  };

  group('결제 요청', () => {
    const orderId = `ORDER-${__VU}-${__ITER}-${Date.now()}`;
    const payload = JSON.stringify({
      orderId: orderId,
      cardType: 'SAMSUNG',
      cardNo: '1234-5678-9012-3456',
      amount: 50000,
    });

    const res = http.post(`${BASE_URL}/api/v1/payments`, payload, { headers });

    responseTime.add(res.timings.duration);

    const isOk = check(res, {
      '응답 상태 200': (r) => r.status === 200,
      '응답 본문 존재': (r) => r.body && r.body.length > 0,
    });

    successRate.add(isOk);

    if (res.status === 200) {
      try {
        const body = JSON.parse(res.body);
        const status = body.data?.status;

        if (status === 'PENDING') {
          paymentPending.add(1);
        } else if (status === 'REQUESTED') {
          paymentFallback.add(1);
        }
        paymentRequested.add(1);
      } catch (e) {
        // JSON 파싱 실패 무시
      }
    }
  });

  sleep(0.1 + Math.random() * 0.2); // 100~300ms 간격
}

export function teardown(data) {
  console.log('=== 부하 테스트 완료 ===');
  console.log('Grafana 대시보드에서 서킷브레이커 상태 전이를 확인하세요');
  console.log('  → http://localhost:3000');
  console.log('Prometheus 메트릭 직접 확인:');
  console.log('  → http://localhost:9090/graph?g0.expr=resilience4j_circuitbreaker_state');
}
