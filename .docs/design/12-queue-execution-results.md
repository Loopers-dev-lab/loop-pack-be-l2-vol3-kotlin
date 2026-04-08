# 12. Queue Execution Test Results

## 목적

이 문서는 이미 실행된 queue 전략 비교 테스트의 결과를
다시 읽기 쉽게 정리한 실행 기록이다.

핵심은 두 가지다.

1. **어떤 전략이 실제 실행에서 어떻게 보였는지**
2. **어떤 전략은 정상 완료됐고, 어떤 전략은 실제로 깨졌는지**

---

## 실행 증거

실행 결과는 아래 파일에 남아 있다.

- 요약 표: `k6/results/comparison-table.md`
- 전략별 raw summary:
  - `k6/results/REDIS_ONLY.summary.json`
  - `k6/results/REDIS_KAFKA.summary.json`
  - `k6/results/KAFKA_ONLY.summary.json`
  - `k6/results/PESSIMISTIC_LOCK.summary.json`
  - `k6/results/DISTRIBUTED_LOCK.summary.json`
- 앱 로그:
  - `k6/results/app-logs/REDIS_ONLY.log`
  - `k6/results/app-logs/REDIS_KAFKA.log`
  - `k6/results/app-logs/KAFKA_ONLY.log`
  - `k6/results/app-logs/PESSIMISTIC_LOCK.log`
  - `k6/results/app-logs/DISTRIBUTED_LOCK.log`
- 테스트 스크립트: `k6/scripts/queue-strategy-comparison.js`
- queue 단위 테스트 리포트: `apps/commerce-api/build/reports/tests/test/packages/com.loopers.application.queue.html`

---

## 이번 실행의 공통 시나리오

`k6/scripts/queue-strategy-comparison.js` 기준으로,
이번 비교는 아래 흐름을 한 사이클로 실행했다.

1. 사용자 생성
2. `POST /api/v1/queue/enter`
3. `GET /api/v1/queue/position` polling
4. 토큰 확보 후 `POST /api/v1/orders`

기본 실행 파라미터는 이렇다.

- 사용자 수: `40`
- 상품 수: `5`
- polling 간격: `0.2s`
- 최대 polling 횟수: `25`
- maxDuration: `2m`

앱 기본 queue 설정은 `application.yml` 기준으로 아래였다.

- active strategy: `REDIS_ONLY` (전략별 실행 시에는 개별 run에서 교체)
- token TTL: `5m`
- scheduler fixed delay: `1s`
- avg order processing time: `2s`
- db connection pool size: `40`
- db utilization ratio: `0.7`

---

## 결과 요약 표

`k6/results/comparison-table.md` 기준 요약은 아래와 같다.

| 전략 | order_success | token_timeout | errors | http p95(ms) | enter p95(ms) | poll p95(ms) | order p95(ms) | e2e p95(ms) | wait avg(s) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| REDIS_ONLY | 40 | 0 | 0 | 228.04 | 256.83 | 208.00 | 204.09 | 3606.05 | 1.93 |
| REDIS_KAFKA | 40 | 0 | 0 | 235.81 | 249.93 | 172.47 | 126.61 | 4642.10 | 3.00 |
| KAFKA_ONLY | 40 | 0 | 0 | 811.53 | 817.97 | 259.11 | 154.72 | 4581.00 | 2.97 |
| DISTRIBUTED_LOCK | 40 | 0 | 0 | 243.51 | 278.91 | 169.74 | 130.64 | 4317.25 | 2.70 |
| PESSIMISTIC_LOCK | 0 | 40 | 0.04 | 192.37 | 259.39 | 181.64 | 0 | 0 | 0 |

---

## 한눈에 보는 결론

### 1. REDIS_ONLY

- 가장 단순한 기준선으로 읽기 좋다.
- 40명 기준에서는 전원 주문 성공.
- 평균 대기 시간과 end-to-end 시간이 가장 짧은 편이다.

이 결과는 **"가장 이해하기 쉬운 FIFO 대기열 기준선"** 으로 쓰기 좋다.

### 2. REDIS_KAFKA

- 전원 주문 성공.
- HTTP/queue latency는 크게 나쁘지 않다.
- 다만 end-to-end 대기 시간은 `REDIS_ONLY`보다 길다.

즉, **상태 전달을 분리한 대가로 체감 대기 시간이 늘어나는 구조**로 읽는 게 맞다.

### 3. KAFKA_ONLY

- 전원 주문 성공은 했지만,
  enter p95와 http p95가 다른 전략보다 확실히 높다.
- 특히 `queue_enter_duration p95 ~= 818ms`는
  같은 조건에서 사용자 첫 응답 체감이 가장 나쁜 축이다.

즉, **이벤트 스트림으로는 성립하지만 대기열 UX 관문으로는 무겁다**는 인상을 준다.

### 4. DISTRIBUTED_LOCK

- 전원 주문 성공.
- REDIS_ONLY보다 느리지만, 전체적으로는 안정적으로 끝났다.
- lock coordination 비용이 붙었지만 이번 부하에서는 완전히 무너지진 않았다.

즉, **멀티 인스턴스 경쟁 제어를 감수하고 안정성 쪽으로 기운 구조**로 읽을 수 있다.

### 5. PESSIMISTIC_LOCK

- 이번 실행의 명확한 실패 전략이다.
- `order_success = 0`, `token_timeout = 40`.
- 앱 로그에 스케줄러 예외가 반복적으로 찍혔다.

실제 로그에 남은 핵심 오류:

- `Unexpected error occurred in scheduled task`
- `InvalidDataAccessApiUsageException`
- `Illegal attempt to set lock mode for a native query`

즉, 이 run에서는 단순히 "느리다"가 아니라,
**admission 자체가 정상 동작하지 않아 토큰 발급 단계에서 막혔다**고 보는 게 정확하다.

---

## 전략별 상세 해석

## REDIS_ONLY

### 관찰

- `queue_order_success = 40`
- `queue_wait_seconds avg ~= 1.93s`
- `queue_end_to_end_duration p95 ~= 3606ms`

### 해석

- 가장 빠른 end-to-end 기준선
- polling p95도 감당 가능한 수준
- 구조가 단순해서 결과 해석이 쉽다

### 눈으로 보면 보일 것

- 순번이 비교적 자연스럽게 줄어든다
- 토큰 전환 후 주문 성공까지 큰 흔들림이 적다

## REDIS_KAFKA

### 관찰

- `queue_order_success = 40`
- `queue_wait_seconds avg ~= 3.00s`
- `queue_end_to_end_duration p95 ~= 4642ms`

### 해석

- 성공률은 좋지만 waiting -> admitted 전환 비용이 붙는다
- 사용자 체감상 "잘 되긴 되는데 조금 더 기다린다" 쪽이다

### 눈으로 보면 보일 것

- position polling은 안정적이지만
  admitted 전환 타이밍이 REDIS_ONLY보다 조금 더 늦게 체감될 수 있다

## KAFKA_ONLY

### 관찰

- `queue_order_success = 40`
- `http_req_duration p95 ~= 812ms`
- `queue_enter_duration p95 ~= 818ms`

### 해석

- 첫 진입 응답부터 무겁다
- 성공은 하지만, 사용자 입장에서 "대기열 진입 자체가 답답한 전략"으로 보일 수 있다

### 눈으로 보면 보일 것

- `/queue/enter` 응답이 다른 전략보다 늦다
- 순번 조회보다 진입 순간의 둔감함이 먼저 느껴진다

## DISTRIBUTED_LOCK

### 관찰

- `queue_order_success = 40`
- `queue_wait_seconds avg ~= 2.70s`
- `queue_end_to_end_duration p95 ~= 4317ms`

### 해석

- coordination 비용은 있지만 이번 부하에서는 안정적
- REDIS_ONLY보다는 무겁고, KAFKA_ONLY보다는 첫 응답이 낫다

### 눈으로 보면 보일 것

- 잘 돌아가지만 완전히 가볍진 않다
- 다중 스케줄러/다중 인스턴스 대비용 구조라는 느낌이 난다

## PESSIMISTIC_LOCK

### 관찰

- `queue_order_success = 0`
- `queue_token_timeout = 40`
- `errors ~= 0.04`
- 로그에서 스케줄러 에러 반복

### 해석

- 이건 성능 비교 이전에 기능 실패다
- polling은 되지만 admitted로 넘어가지 못해 주문까지 못 간다

### 눈으로 보면 보일 것

- `/queue/position`은 계속 WAITING처럼 보이는데 주문으로 못 넘어간다
- 앱 로그에 스케줄러 예외가 1초 단위로 반복된다

---

## 체크리스트 관점에서 이번 결과를 어떻게 읽을까

붙여주신 요구사항 기준으로 보면,
이번 실행은 아래처럼 읽는 게 맞다.

### Step 1 — Redis 기반 대기열 구현

- `POST /queue/enter`: 구현 및 실행 흔적 확인 가능
- `GET /queue/position`: 구현 및 polling 실행 흔적 확인 가능
- 중복 진입 방지: 코드상 전략별 처리 존재하지만, **이번 k6 run은 같은 user 동시 중복 진입 실험이 아님**
- 전체 대기 인원 조회: 응답 DTO의 `totalWaitingCount` 필드로 노출됨

### Step 2 — 입장 토큰 & 스케줄러

- 스케줄러 admission: 대부분 전략에서 실행 확인
- 토큰 기반 주문 진입: k6가 `X-Queue-Token`, `X-Queue-Strategy` 헤더로 주문 호출
- 토큰 TTL 설정: 설정값 `5m` 확인 가능
- 주문 완료 후 토큰 삭제: 코드 경로 존재
- 배치 크기 산정 근거: `.docs/design/10-queue-experiment-comparison.md`에 공식 문서화됨

단, `PESSIMISTIC_LOCK`는 이번 실행에서 스케줄러 경로가 깨져서
체크리스트를 통과했다고 볼 수 없다.

### Step 3 — 실시간 순번 조회

- polling 기반 순번 조회: 실행됨
- 예상 대기 시간: DTO `expectedWaitSeconds` 존재
- 토큰 발급 시 응답 내 토큰 포함: DTO `token`, `tokenExpiresAt` 존재

### 검증

- 동시 진입: 40명 shared-iterations 실행으로 일부 검증됨
- 토큰 만료 테스트: **이번 run은 timeout 관찰이 일부 있었지만 TTL 만료 실험으로 설계된 run은 아님**
- 처리량 초과 테스트: **이번 run은 학습/비교용 40명 기준선이라 과부하 한계 실험까지는 아님**

---

## 이번 실행에서 드러난 가장 큰 문제

이번 비교 결과를 한 줄로 요약하면 이렇다.

> **정상 비교가 가능한 전략 4개와, 실행 단계에서 이미 깨진 전략 1개가 있었다.**

가장 큰 문제는 `PESSIMISTIC_LOCK`였다.

- 증상: 모든 사용자가 토큰을 못 받고 timeout
- 사용자 눈에는: "계속 기다리는데 입장이 안 됨"
- 서버 눈에는: 스케줄러가 반복 예외로 admission 실패
- 원인 단서: native query에 lock mode를 거는 구간에서 예외 발생

즉, 이 전략은 지금 상태에서 **느린 전략**이 아니라
**실험 하네스 기준으로 기능이 깨진 전략**으로 분류해야 한다.

---

## 보수적으로 내려야 할 결론

이번 실행만으로 안전하게 말할 수 있는 결론은 아래 정도다.

- `REDIS_ONLY`는 현재 기준선으로 가장 읽기 쉽고 빠르다
- `REDIS_KAFKA`와 `DISTRIBUTED_LOCK`는 성공률은 유지하지만 end-to-end 비용이 늘어난다
- `KAFKA_ONLY`는 성공은 해도 queue entrance UX가 무겁다
- `PESSIMISTIC_LOCK`는 현재 구현/실행 경로에 기능적 결함이 있다

반대로 아직 말하면 안 되는 결론도 있다.

- "실서비스는 무조건 REDIS_ONLY가 정답이다"
- "KAFKA_ONLY는 절대 못 쓴다"
- "DB 기반 전략은 항상 실패한다"

왜냐하면 이번 run은 **40명 학습용 기준선**이고,
과부하 구간과 반복 run이 아직 부족하기 때문이다.

---

## 보강하면 좋은 다음 실험

다음에는 아래 세 가지를 따로 돌려야 한다.

1. **중복 진입 테스트**
   - 같은 user가 동시에 여러 번 enter
2. **TTL 만료 테스트**
   - 토큰 받은 뒤 주문하지 않고 만료 대기
3. **처리량 초과 테스트**
   - 40명이 아니라 200, 500, 1000으로 올려 병목 지점 확인

이 세 가지를 붙이면 지금 문서는 단순 결과 요약이 아니라
실제 설계 의사결정 문서로 쓸 수 있다.
