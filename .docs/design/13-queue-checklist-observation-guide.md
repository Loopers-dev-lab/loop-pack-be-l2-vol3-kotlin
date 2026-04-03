# 13. Queue Checklist Observation Guide

## 목적

이 문서는 요구사항 체크리스트를
단순 구현 확인표가 아니라,
**직접 테스트하면서 눈으로 문제를 확인하는 실험표**로 바꿔놓은 문서다.

즉, 각 항목마다 아래를 연결한다.

- 무엇을 기대해야 하는가
- 어떻게 테스트해야 하는가
- 어디를 봐야 하는가
- 깨지면 어떤 증상이 보이는가

---

## 먼저 알아둘 것

이번 레포에서 사용자가 직접 보게 될 관찰 지점은 네 군데다.

1. **API 응답**
   - `/api/v1/queue/enter`
   - `/api/v1/queue/position`
   - `/api/v1/orders`
2. **k6 결과**
   - `k6/results/*.summary.json`
   - `k6/results/comparison-table.md`
3. **앱 로그**
   - `k6/results/app-logs/*.log`
4. **Grafana / Prometheus**
   - HTTP p95, error rate, 자원 사용량

테스트는 항상 아래 순서로 읽는다.

> **응답 -> 로그 -> 요약 수치 -> 자원 그래프**

---

## Step 1 — 대기열

### [ ] Redis Sorted Set 기반 대기열 진입 API 구현 (`POST /queue/enter`)

### 기대 동작

- enter 호출 즉시 현재 상태가 내려온다
- 응답에 strategy, state, position, totalWaitingCount, expectedWaitSeconds가 보인다

### 어디서 확인하나

- API: `POST /api/v1/queue/enter`
- 응답 필드: `QueueV1Dto.PositionResponse`

### 어떻게 테스트하나

1. 사용자 1명으로 진입
2. 사용자 10명으로 동시 진입
3. 사용자 40명 이상으로 진입

### 내 눈에 보일 정상 신호

- 응답이 바로 온다
- 첫 응답부터 `position`이 비정상적으로 비어 있지 않다
- `totalWaitingCount`가 증가한다

### 깨지면 보일 증상

- enter 응답이 과하게 늦다
- `position`이 비거나 튄다
- 진입 성공은 했는데 뒤 조회와 상태가 안 맞는다

### 이번 실행에서 본 신호

- `REDIS_ONLY`, `REDIS_KAFKA`, `DISTRIBUTED_LOCK`는 enter가 안정적이었다
- `KAFKA_ONLY`는 enter p95가 높아 첫 진입 체감이 가장 무거웠다

---

### [ ] 순번 조회 API 구현 (`GET /queue/position`)

### 기대 동작

- 대기 중이면 순번이 내려온다
- admitted 되면 `canEnterOrderApi=true`와 token이 내려온다

### 어디서 확인하나

- API: `GET /api/v1/queue/position`
- 응답 필드: `position`, `expectedWaitSeconds`, `canEnterOrderApi`, `token`, `tokenExpiresAt`

### 어떻게 테스트하나

1. enter 후 0.2초~1초 간격 polling
2. 대기 인원이 적을 때/많을 때 각각 비교

### 내 눈에 보일 정상 신호

- 순번이 대체로 감소 방향으로 움직인다
- admitted 시점에 token이 채워진다

### 깨지면 보일 증상

- 순번이 오르락내리락한다
- admitted 되었는데 token이 없다
- 계속 WAITING인데 실제로는 스케줄러가 죽어 있다

### 이번 실행에서 본 신호

- 대부분 전략은 polling이 정상적으로 이어졌다
- `PESSIMISTIC_LOCK`는 polling은 되지만 토큰 발급 단계로 못 넘어가 timeout처럼 보였다

---

### [ ] userId 기반 중복 진입 방지

### 기대 동작

- 같은 user가 여러 번 enter해도 active entry는 1개만 유지된다

### 어떻게 테스트하나

1. 같은 계정으로 동시에 `POST /queue/enter` 10번 호출
2. 이후 `GET /queue/position` 확인
3. DB/Redis 저장 상태도 함께 확인

### 내 눈에 보일 정상 신호

- 순번이 하나만 유지된다
- total waiting count가 불필요하게 늘지 않는다

### 깨지면 보일 증상

- 같은 user가 여러 순번을 차지한다
- admitted 이후에도 잔여 엔트리가 남는다
- polling 결과가 한 번은 WAITING, 한 번은 ADMITTED처럼 흔들린다

### 이번 실행에서 본 신호

- 이번 k6 시나리오는 **서로 다른 40명 사용자 생성**이라
  이 항목은 직접 검증되지 않았다.

즉, 이건 **추가 실험이 꼭 필요**하다.

---

### [ ] 전체 대기 인원 조회

### 기대 동작

- 사용자는 현재 전체 대기 인원 규모를 알 수 있다

### 어디서 확인하나

- `GET /queue/position` 응답의 `totalWaitingCount`

### 어떻게 테스트하나

1. 1명, 10명, 40명 순서로 진입
2. 각 유저 응답의 `totalWaitingCount` 비교

### 깨지면 보일 증상

- position은 맞는데 전체 인원 수가 일관되지 않다
- polling마다 waiting count가 비정상적으로 흔들린다

---

## Step 2 — 입장 토큰 & 스케줄러

### [ ] 스케줄러가 주기적으로 대기열에서 N명을 꺼내 입장 토큰 발급

### 기대 동작

- fixed-delay마다 waiting -> admitted 전환이 발생한다
- batch size 범위 내에서 토큰이 발급된다

### 어디서 확인하나

- `/queue/position` polling 결과
- 앱 로그
- `comparison-table.md`의 success / timeout 수치

### 어떻게 테스트하나

1. 40명 진입
2. polling으로 admitted 전환 타이밍 관찰
3. batch size보다 큰 요청으로 몇 번에 나눠 admission 되는지 확인

### 내 눈에 보일 정상 신호

- 1초 단위 정도로 admitted 전환이 생긴다
- 일부 사용자가 먼저 token을 받는다

### 깨지면 보일 증상

- 모두 WAITING에 머문다
- 토큰 발급이 전혀 안 일어난다
- 로그에 scheduled task error가 반복된다

### 이번 실행에서 본 신호

- `PESSIMISTIC_LOCK`에서 이 항목이 명확히 깨졌다
- 실제 로그에 `Unexpected error occurred in scheduled task`가 반복 기록됐다

---

### [ ] 토큰 TTL 설정 (e.g. 5분)

### 기대 동작

- admitted 이후 제한 시간 내 주문하지 않으면 토큰이 만료된다

### 어디서 확인하나

- 설정: `application.yml`의 `queue.experiment.token-ttl: 5m`
- 응답: `tokenExpiresAt`

### 어떻게 테스트하나

1. 토큰을 받은 뒤 주문하지 않고 기다린다
2. TTL 이후 주문 호출
3. 다시 polling하여 상태가 어떻게 바뀌는지 본다

### 깨지면 보일 증상

- 만료 후에도 주문이 된다
- 만료됐는데도 polling은 admitted 상태로 남아 있다
- 만료 후 재진입이 이상하게 막힌다

### 이번 실행에서 본 신호

- TTL 설정값과 응답 필드는 확인 가능
- 하지만 **의도적인 TTL 만료 실험은 이번 run에 포함되지 않았다**

---

### [ ] 주문 API 진입 시 토큰 검증

### 기대 동작

- 올바른 토큰이면 주문 성공
- 토큰이 없거나 잘못되면 차단

### 어디서 확인하나

- 주문 API 헤더: `X-Queue-Token`, `X-Queue-Strategy`
- 주문 컨트롤러: queue gate validate 후 order 생성

### 어떻게 테스트하나

1. 정상 토큰으로 주문
2. 토큰 없이 주문
3. 다른 사용자 토큰으로 주문
4. 만료 토큰으로 주문

### 내 눈에 보일 정상 신호

- 정상 토큰만 통과한다
- 비정상 토큰은 403/400으로 막힌다

### 깨지면 보일 증상

- 토큰 없이도 주문된다
- 다른 사용자 토큰으로도 주문된다
- admitted까지 됐는데 정상 토큰이 막힌다

### 이번 실행에서 본 신호

- k6는 정상 토큰 흐름으로 주문 성공 여부를 확인했다
- 비정상 토큰 케이스는 별도 부정 테스트가 더 필요하다

---

### [ ] 주문 완료 후 토큰 삭제

### 기대 동작

- 주문 완료 후 같은 토큰 재사용이 안 된다

### 어떻게 테스트하나

1. 정상 주문 1회
2. 같은 토큰으로 주문 재시도

### 깨지면 보일 증상

- 같은 토큰으로 중복 주문이 된다
- polling에서 이미 완료된 사용자가 계속 admitted처럼 보인다

---

### [ ] 처리량 기준으로 스케줄러 배치 크기 산정 근거 문서화

### 어디서 확인하나

- `.docs/design/10-queue-experiment-comparison.md`

### 이번 레포 기준 근거

- DB pool size: `40`
- utilization ratio: `0.7`
- scheduler interval: `1s`
- avg order processing time: `2s`
- 계산 결과: tick당 `14`

### 내 눈으로 확인하려면

- waiting user를 14명보다 조금 많은 수로 넣는다
- 1 tick에서 일부만 admitted 되는지 본다
- 다음 tick에서 나머지가 이어서 admitted 되는지 본다

---

## Step 3 — 실시간 순번 조회

### [ ] 예상 대기 시간 계산 로직 구현

### 기대 동작

- `expectedWaitSeconds`가 0이 아닌 의미 있는 값으로 내려온다

### 어떻게 테스트하나

1. 5명, 20명, 40명으로 늘려가며 enter
2. position과 expected wait가 함께 늘어나는지 본다

### 깨지면 보일 증상

- position은 큰데 wait 시간이 계속 0이다
- admitted 직전인데 wait 시간이 비정상적으로 크다

---

### [ ] Polling 기반 순번 + 예상 대기 시간 응답

### 기대 동작

- polling할수록 순번은 줄고 wait도 줄어든다

### 어떻게 테스트하나

1. polling 주기를 0.2초, 1초, 2초로 바꿔본다
2. 응답 지연과 사용자 체감을 같이 기록한다

### 깨지면 보일 증상

- polling이 잦아질수록 응답 p95가 급증한다
- 값은 바뀌는데 사용자는 admitted 타이밍을 이해하기 어렵다

### 이번 실행에서 본 신호

- `queue_poll_duration p95`는 대부분 170~260ms 구간
- KAFKA_ONLY는 enter가 특히 무거웠고, polling은 상대적으로 덜 나빴다

---

### [ ] 토큰 발급 시 순번 조회 응답에 토큰 포함

### 기대 동작

- 상태가 admitted로 바뀌는 순간 `token`, `tokenExpiresAt`가 함께 내려온다

### 어디서 확인하나

- `QueueV1Dto.PositionResponse`

### 깨지면 보일 증상

- `canEnterOrderApi=true`인데 token이 null
- token은 있는데 주문이 계속 forbidden

---

## 검증

### [ ] 동시 진입 테스트 — 대기열 순서가 정확히 보장되는지 확인

### 어떻게 봐야 하나

- 단순 성공률만 보지 말고
  **몇 번째로 들어온 사용자가 먼저 admitted 되는지**를 본다

### 눈으로 확인하는 방법

- 소수 인원(5~10명)으로 먼저 테스트
- 각 사용자 진입 시각을 기록
- admitted 순서를 비교

### 깨지면 보일 증상

- 늦게 들어온 사용자가 먼저 admitted 된다
- 같은 시점이라도 순번이 들쭉날쭉하다

### 이번 실행에서 본 수준

- 40명 비교 실험으로 admission 흐름은 관찰했지만,
  **정밀 FIFO 검증 run**이라고 보긴 어렵다

---

### [ ] 토큰 만료 테스트 — TTL 초과 시 토큰이 무효화되는지 확인

### 눈으로 확인하는 방법

1. admitted 후 주문하지 않는다
2. `tokenExpiresAt` 시각이 지난 뒤 주문한다
3. 실패 응답과 상태 변화를 같이 본다

### 깨지면 보일 증상

- 만료 토큰으로 주문이 성공한다
- 만료됐는데도 queue position은 admitted로 남아 있다

---

### [ ] 처리량 초과 테스트 — 스케줄러 배치 크기 이상의 요청이 들어와도 안정적인지 확인

### 눈으로 확인하는 방법

1. 40명 기준선
2. 200명
3. 500명
4. 1000명

각 구간마다 아래를 적는다.

- enter가 먼저 느려지는가
- polling이 먼저 느려지는가
- 주문이 막히는가
- 로그에 예외가 뜨는가
- Grafana에서 p95/p99가 어디서 꺾이는가

### 전략별로 예상되는 대표 증상

- `REDIS_ONLY`: polling 증가 시 Redis/HTTP 조회 부담이 먼저 보일 수 있음
- `REDIS_KAFKA`: admitted 전환 지연이 길어질 수 있음
- `KAFKA_ONLY`: enter 응답부터 둔감해질 수 있음
- `DISTRIBUTED_LOCK`: coordination 비용이 tail latency로 드러날 수 있음
- `PESSIMISTIC_LOCK`: DB lock/admission 경로가 먼저 깨질 수 있음

---

## 이번 결과 기준으로 특히 주의할 문제

### 1. "느리다"와 "깨졌다"를 구분해야 한다

- `KAFKA_ONLY`는 느린 편이지만 성공했다
- `PESSIMISTIC_LOCK`는 느린 게 아니라 실제로 admission 경로가 깨졌다

### 2. polling이 된다고 시스템이 정상은 아니다

`PESSIMISTIC_LOCK`처럼 polling은 계속 되는데
토큰 발급이 안 되는 경우가 있다.

즉,

> **WAITING이 계속 보인다고 해서 queue가 건강한 게 아니다.**

### 3. 첫 응답이 느린 전략은 사용자 체감이 급격히 나빠진다

`KAFKA_ONLY`처럼 enter 자체가 800ms대까지 오르면
사용자는 "대기열에 들어가는 것부터 무겁다"고 느낀다.

---

## 추천 실습 순서

학습 목적이라면 아래 순서가 좋다.

1. `REDIS_ONLY`
2. `PESSIMISTIC_LOCK`
3. `DISTRIBUTED_LOCK`
4. `REDIS_KAFKA`
5. `KAFKA_ONLY`

이 순서가 좋은 이유는,
문제가 어디서 나는지 몸으로 구분하기 쉽기 때문이다.

- 단순 queue
- DB lock 병목
- 분산 lock 비용
- async 전달 trade-off
- event stream 기반 UX 한계

---

## 마지막으로, 테스트 끝날 때 꼭 적을 질문

각 전략마다 아래 다섯 줄은 꼭 직접 적는다.

1. 어디서 가장 먼저 이상 징후가 보였는가
2. 그 이상 징후는 응답, 로그, 그래프 중 어디에서 먼저 보였는가
3. 이 전략은 실패할 때 사용자에게 어떻게 보였는가
4. 이 전략은 실패 원인을 운영자가 빨리 찾기 쉬운가
5. 이 전략은 지금 팀이 운영할 만한 복잡도인가

이 다섯 줄을 남기면,
테스트가 단순 성공/실패 확인이 아니라
**진짜 학습 실험**이 된다.
