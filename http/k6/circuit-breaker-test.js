import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

// ============================================================
// 서킷브레이커 상태 전이 테스트
//
// 사전 조건:
//   1. commerce-api (8080) 실행
//   2. pg-simulator 끄기 ← 핵심! (모든 PG 호출 실패하게)
//   3. seed SQL 실행 + 회원가입 완료
//
// 실행:
//   k6 run http/k6/circuit-breaker-test.js
//
// 기대 결과:
//   1단계: 3건 실패 → 서킷 OPEN
//   2단계: 이후 요청 → 503 (PG_CIRCUIT_OPEN, PG 호출 없이 즉시 차단)
//   3단계: 30초 후 → HALF_OPEN → 시험 호출 → 실패 → 다시 OPEN
// ============================================================

const circuitOpen = new Counter('circuit_open_count');
const pgError = new Counter('pg_error_count');

export const options = {
  scenarios: {
    steady: {
      executor: 'constant-arrival-rate',
      rate: 2,                 // 초당 2건 (천천히)
      timeUnit: '1s',
      duration: '60s',         // 1분간 (OPEN → HALF_OPEN 전이까지 관찰)
      preAllocatedVUs: 10,
      maxVUs: 20,
    },
  },
};

let orderCounter = 0;

export default function () {
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

  if (res.status === 503) {
    circuitOpen.add(1);
  } else if (res.status === 502 || res.status === 504) {
    pgError.add(1);
  }

  // 매 요청마다 서킷 상태 확인 (로그용)
  const cbRes = http.get('http://localhost:8081/actuator/circuitbreakers');
  const cbData = JSON.parse(cbRes.body);
  const pgRequest = cbData.circuitBreakers['pg-request'];
  if (pgRequest) {
    console.log(
      `[${new Date().toISOString()}] state=${pgRequest.state} buffered=${pgRequest.bufferedCalls} failed=${pgRequest.failedCalls} notPermitted=${pgRequest.notPermittedCalls} failureRate=${pgRequest.failureRate}`
    );
  }
}

export function teardown() {
  const cbRes = http.get('http://localhost:8081/actuator/circuitbreakers');
  console.log('=== 서킷브레이커 최종 상태 ===');
  console.log(cbRes.body);
}
