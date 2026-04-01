# Round 8 — 기술 의사결정 기록

Redis 기반 대기열 시스템 구현 과정에서 멘토링 피드백과 AI 리뷰를 반영한 설계 결정을 기록한다.

---

## 1. Redis 장애 시 Fallback 전략: Fail-Open → Fail-Fast 전환

### 문제

Redis 장애 시 대기열/토큰 검증을 우회하여 주문을 허용하는 Fail-Open 전략을 구현했으나, 멘토링에서 이 접근이 위험하다는 피드백을 받았다.

### 현재 구현 (AS-IS)

`QueueFallbackHandler`가 `AtomicBoolean`으로 Redis 상태를 추적하고, 장애 시 `EntryTokenInterceptor`가 토큰 검증을 건너뛰어 주문을 허용한다.

### 원인 (왜 변경해야 하는가)

멘토 피드백 (Section 8): *"어설프게 우회로를 뚫기보다, 차라리 **전면 리젝(Fail-fast)**을 통해 유입을 완전히 차단하고 DB를 보호하는 것이 최선"*

- Fail-Open 시 대기열 없이 전체 트래픽이 DB/결제 시스템에 직접 도달
- DB 과부하 → 결제 시스템 과부하 → 연쇄 장애(Cascading Failure)
- 대기열을 도입한 목적(Back-pressure) 자체가 무력화됨

### 해결방안

| 방안 | 설명 | 장점 | 단점 |
|------|------|------|------|
| A. Fail-Open (현행) | Redis 장애 시 대기열 우회, 주문 허용 | 서비스 가용성 유지 | DB 연쇄 장애 위험, 대기열 목적 무력화 |
| B. Fail-Fast | Redis 장애 시 전면 리젝 (503 반환) | DB 보호, 사고 범위 축소 | 일시적 서비스 불가 |
| C. 로컬 Rate Limiter 절충 | Redis 장애 시 로컬 메모리 Token Bucket으로 극소량만 허용 | DB 보호 + 최소 가용성 | 구현 복잡도 증가, 서버별 허용량 계산 필요 |

### 최종 선택

**B. Fail-Fast**

### 근거

- 멘토 권고: 장애 시 문을 닫고 인프라 보호 → 빠른 복구가 최선
- C(로컬 Rate Limiter)는 멘토 Section 9에서 "최후의 보루"로 언급했으나, 학습 프로젝트에서 구현 복잡도 대비 실익이 적음
- Redis 자체의 고가용성(Scale-up, 리소스 압도적 할당)이 정석이며, fallback은 보조 수단
- Fail-Fast는 구현이 단순하고 의도가 명확함 (503 + 재시도 안내)

### 변경 범위

- `QueueFallbackHandler`: `markUnavailable()` 시 bypass 대신 reject 모드 전환
- `EntryTokenInterceptor`: fallback 상태에서 bypass 대신 503 반환
- `QueueScheduler`: 예외 분류 (인프라 예외만 fallback 처리, 비인프라 예외는 rethrow)
- 관련 테스트 업데이트

---

## 2. 토큰 발급 흐름: ZPOPMIN(Pop-First) → 조회 후 삭제(Safe Pattern) 전환

### 문제

현재 `IssueEntryTokensUseCase`가 `ZPOPMIN`으로 대기열에서 사용자를 먼저 제거한 후 토큰을 발급한다. 토큰 발급 중 장애 발생 시 사용자가 대기열에서 사라지면서 토큰도 받지 못하는 유실 상태가 된다.

### 현재 구현 (AS-IS)

```
ZPOPMIN(batchSize) → userIds 추출 → 각 userId에 토큰 발급 → SSE Push
```

장애 발생 시: 대기열에서 빠진 사용자가 토큰 없이 증발.

### 원인 (왜 변경해야 하는가)

멘토 피드백 (Section 1, 3, 17): *"'절대로 데이터를 먼저 삭제(Pop)하지 않는 원칙'을 지키는 것이 더 중요합니다"*

- Pop-First는 장애 시 유저 유실이라는 최악의 시나리오 발생
- 멘토 제안 Safe Pattern: `조회 → 토큰 발급 확인 → 삭제` 순서
- Lua 스크립트로 원자적 처리하거나, 포인터 이동 방식 사용

### 해결방안

| 방안 | 설명 | 장점 | 단점 |
|------|------|------|------|
| A. ZPOPMIN + try-catch 보상 (현행 개선) | Pop 후 실패 시 requeue | 최소 변경 | 원래 score 복원 어려움, race condition |
| B. Lua 스크립트 원자적 처리 | ZRANGE → SET 토큰 → ZREM을 Lua로 묶기 | 원자성 보장, 유실 없음 | Lua 스크립트 복잡도 증가, 디버깅 어려움 |
| C. 조회 후 삭제 (ZRANGE → 발급 → ZREM) | 조회로 후보 확인 → 토큰 발급 → 확인 후 삭제 | 유실 없음, 재시도 안전 | 비원자적, 중복 발급 가능 (Set으로 방어) |
| D. 포인터 이동 방식 | 데이터 삭제 없이 포인터만 이동하여 입장열로 전환 | 데이터 안전, 이동 비용 최소 | 설계 변경 큼, 별도 포인터 관리 필요 |

### 최종 선택

**B. Lua 스크립트 원자적 처리**

### 근거

- 멘토 Section 1: *"가장 선호하는 방식은 Lua 스크립트를 활용해 Redis 내부에서 원자적 트랜잭션으로 처리"*
- 이미 `RedisWaitingQueueRepository.enter()`에서 Lua 스크립트를 사용 중이므로 패턴에 익숙
- C는 비원자적이라 동시 스케줄러 실행 시 중복 발급 위험 (멘토 Section 2 참고)
- D는 설계 변경이 과대하여 현 단계에서는 과잉
- A는 근본 원인(Pop-First)을 해결하지 않음

### 변경 범위

- `RedisWaitingQueueRepository`: `popMin()` → `popMinAndIssueTokens()` Lua 스크립트 (ZRANGE → SET 토큰 → ZREM 원자적)
- `IssueEntryTokensUseCase`: 새 메서드 호출로 변경
- `WaitingQueueRepository` 인터페이스: 시그니처 변경
- `FakeWaitingQueueRepository`: 테스트용 구현 동기화
- Lua 스크립트 파일 추가

---

## 3. 실시간 통신: SSE 유지 여부

### 문제

대기열 순번 알림을 위해 SSE(Server-Sent Events)를 구현했으나, 멘토링에서 대기열 시나리오에 SSE가 적합하지 않다는 의견을 받았다.

### 현재 구현 (AS-IS)

`GET /queue/events` SSE 엔드포인트로 토큰 발급/순번 변경을 실시간 Push.

### 멘토 의견

멘토 피드백 (Section 14): *"100만 명의 커넥션(State)을 서버가 유지하는 것 자체가 엄청난 리소스 낭비. 대기열에서의 SSE 비관론."*

- 대기열은 "명확한 끝"이 있는 시나리오 → Polling이 적합
- SSE는 주식 시세, 채팅처럼 끝이 없는 무한 Push에 적합
- 서버도 내부적으로 주기적 갱신 후 Push하므로 Polling과 실질적 차이가 적음

### 해결방안

| 방안 | 설명 | 장점 | 단점 |
|------|------|------|------|
| A. SSE 제거, Polling만 유지 | 기존 GET /queue/position API로 클라이언트 폴링 | 단순, 리소스 효율적 | 실시간성 약간 저하 |
| B. SSE 유지 (현행) | 학습 목적으로 SSE 구현 유지 | SSE 기술 학습 | 멘토 권고와 불일치, 운영 시 리소스 문제 |
| C. SSE 유지 + 멘토 의견 문서화 | 구현은 유지하되 트레이드오프 기록 | 학습 + 의사결정 기록 | 코드와 설계 의도의 괴리 |

### 최종 선택

**A. SSE 제거, Polling만 유지**

### 근거

- 멘토 피드백이 명확: 대기열처럼 "끝이 있는" 시나리오에서 SSE는 리소스 낭비
- Step 5에서 SSE 구현 경험을 이미 획득 → 학습 목적 달성
- 기존 `GET /queue/position` + `recommendedPollIntervalMs`로 동적 폴링이 충분한 대안
- SSE 제거로 코드 단순화: QueueSseEmitterRegistry, SSE 엔드포인트, 스케줄러 push 로직 삭제 (약 370줄 순삭감)
- QueueScheduler는 토큰 발급 + fallback 상태 관리만 담당하여 책임이 명확해짐

### 변경 범위 (Step 8에서 실행)

- `QueueSseEmitterRegistry.kt` 삭제
- `QueueV1Controller`: `streamQueueEvents` 엔드포인트 제거
- `QueueV1ApiSpec`: SSE 스펙 제거
- `QueueScheduler`: SSE push 로직 전체 제거, 토큰 발급 + fallback만 유지
- `QueueProperties`: `sseTimeoutMs` 필드 제거
- `application.yml`: `sse-timeout-ms` 설정 제거
- 관련 테스트 3개 삭제, 나머지 테스트에서 SSE 의존 제거

---

## 3-1. 토큰 소비 시점: 주문 완료 후 → 인터셉터 검증 즉시

### 문제

AI 리뷰(Codex P1)에서 토큰 재사용 가능성이 지적되었다. 기존에는 `PlaceOrderUseCase`의 `afterCommit`에서 토큰을 삭제했으나, 검증(인터셉터)과 삭제(주문 완료) 사이에 시간 window가 존재하여 동일 토큰으로 다중 요청이 가능했다.

### 해결

`ValidateEntryTokenUseCase`에서 검증 성공 즉시 `entryTokenRepository.delete()` 호출. `PlaceOrderUseCase`에서 토큰 관련 코드 전체 제거.

### 트레이드오프

- `find` → `delete`가 별도 Redis 명령이라 마이크로초 수준의 race window 존재
- 완전한 원자성은 Redis `GETDEL` 또는 Lua 스크립트로 강화 가능 (추후 과제)
- 주문 실패 시 토큰이 이미 소비되어 대기열 재진입 필요 → 시스템 안전성 우선 선택

---

## 3-2. Queue API Fail-Fast: 스케줄러 전용 → UseCase 진입점 확장

### 문제

AI 리뷰(Codex P2)에서 fallback 상태임에도 Queue API가 Redis를 호출하여 불필요한 장애를 유발한다는 점이 지적되었다. 기존에는 `QueueFallbackHandler.isAvailable()` 체크가 스케줄러에서만 수행되었다.

### 해결

`EnterQueueUseCase`, `GetQueuePositionUseCase`의 `execute()` 시작에 `isAvailable()` 체크를 추가. fallback 상태이면 즉시 `SERVICE_UNAVAILABLE(503)` 반환.

### 트레이드오프

- UseCase 실행 도중 Redis 장애가 발생하면 여전히 500이 반환됨 (진입 시점만 체크)
- UseCase 내부에 try-catch를 추가하면 인프라 예외 처리 책임이 유입되므로, 스케줄러(100ms 주기) 기반 감지로 충분하다고 판단

---

## 4. 대기열 적용 범위: 전체 주문 → 특정 대상 한정

### 문제

현재 `EntryTokenInterceptor`가 `/api/v1/orders/**` 전체 경로에 적용되어, GET(주문 조회)까지 토큰 검증 대상이 된다. 또한 멘토링에서는 모든 주문이 아닌 특정 상품에만 대기열을 적용해야 한다는 의견을 받았다.

### 현재 구현 (AS-IS)

`WebMvcConfig`에서 `entryTokenInterceptor`를 `/api/v1/orders/**`에 등록. 모든 HTTP 메서드(GET/POST/PUT 등)에 토큰 검증이 적용됨.

### 원인 (왜 변경해야 하는가)

1. AI 리뷰 (Codex A-03): 주문 완료 후 토큰 삭제 → GET /orders로 본인 주문 조회 불가 → 기능 회귀
2. 멘토 피드백 (Section 4): *"모든 주문이 아니라 트래픽 폭주가 확실한 특정 상품에만 대기열을 적용"*
3. 멘토 피드백 (Section 5): *"'결제하기' 버튼 클릭 등 실질적인 병목 지점에만 대기열 검사를 촘촘하게 거는 것이 운영 효율 면에서 뛰어남"*

### 해결방안

| 방안 | 설명 | 장점 | 단점 |
|------|------|------|------|
| A. POST만 적용 (최소 수정) | 인터셉터를 POST /api/v1/orders에만 적용 | 즉시 적용 가능, GET 회귀 해결 | 여전히 모든 상품에 대기열 적용 |
| B. Feature Flag 기반 상품별 제어 | 어드민에서 대기열 대상 상품을 On/Off | 멘토 권고 방향, 운영 유연성 | 어드민 UI + 상품별 판별 로직 필요 |
| C. A + B 병행 | 먼저 A를 적용하고, B는 후속 과제로 | 즉시 회귀 수정 + 방향성 확보 | B 구현 시점이 불명확 |

### 최종 선택

**A. POST만 적용** (즉시 수정)

### 근거

- GET 조회 차단은 명백한 기능 회귀이므로 즉시 수정 필요
- Feature Flag 기반 상품별 제어(B)는 어드민 시스템이 없는 현 단계에서 scope 밖
- 멘토도 *"리소스가 부족하다면 피처 플래그로 수동 On/Off하는 방식도 충분히 훌륭"*이라고 했으므로, 단계적 접근이 합리적
- 인터셉터 내부에서 HTTP 메서드를 체크하거나, `addPathPatterns`를 조정하여 구현

### 변경 범위

- `WebMvcConfig`: `addPathPatterns` 변경 또는 인터셉터 내 메서드 필터링
