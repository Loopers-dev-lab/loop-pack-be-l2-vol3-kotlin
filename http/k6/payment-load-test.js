import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter } from 'k6/metrics';

// ============================================================
// 결제 API 부하 테스트
//
// 사전 조건:
//   1. commerce-api (8080) + pg-simulator (8082) 실행
//   2. seed SQL 실행 + 0-1 회원가입 완료
//
// 실행:
//   k6 run http/k6/payment-load-test.js
//
// 시나리오:
//   초당 20건 결제 요청 → RateLimiter(10건/초) 초과 확인
//   + 서킷브레이커 상태 변화 관찰
// ============================================================

// 커스텀 메트릭
const paymentSuccess = new Rate('payment_success');
const rateLimited = new Counter('rate_limited');
const circuitOpen = new Counter('circuit_open');

// 부하 설정
export const options = {
  scenarios: {
    // 시나리오 1: 점진적 부하 증가
    ramp_up: {
      executor: 'ramping-arrival-rate',
      startRate: 1,           // 초당 1건에서 시작
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 100,
      stages: [
        { duration: '10s', target: 5 },   // 10초간 초당 5건으로 증가
        { duration: '20s', target: 20 },   // 20초간 초당 20건으로 증가 (RateLimiter 초과)
        { duration: '10s', target: 20 },   // 10초간 유지
        { duration: '10s', target: 0 },    // 10초간 감소
      ],
    },
  },
};

// orderId를 순환 (seed SQL에서 1~20 생성됨)
let orderCounter = 0;

export default function () {
  // orderId 1~20 순환
  orderCounter++;
  const orderId = ((orderCounter - 1) % 20) + 1;

  const payload = JSON.stringify({
    orderId: orderId,
    cardType: 'SAMSUNG',
    cardNo: '1234-5678-9012-3456',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Loopers-LoginId': 'testuser123',
      'X-Loopers-LoginPw': 'Test1234!',
    },
  };

  const res = http.post('http://localhost:8080/api/v1/payments', payload, params);

  // 응답 코드별 분류
  const isSuccess = res.status === 200;
  const isRateLimited = res.status === 429;
  const isCircuitOpen = res.status === 503;

  paymentSuccess.add(isSuccess);
  if (isRateLimited) rateLimited.add(1);
  if (isCircuitOpen) circuitOpen.add(1);

  check(res, {
    'status is 200 or expected error': (r) =>
      r.status === 200 || r.status === 400 || r.status === 429 || r.status === 503,
  });
}

// 테스트 종료 후 서킷 상태 확인
export function teardown() {
  const cbRes = http.get('http://localhost:8081/actuator/circuitbreakers');
  console.log('=== 서킷브레이커 최종 상태 ===');
  console.log(cbRes.body);
}
