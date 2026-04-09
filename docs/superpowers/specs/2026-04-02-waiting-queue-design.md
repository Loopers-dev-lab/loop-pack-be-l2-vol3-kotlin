# [Round 8] Waiting Queue System Design

## Context

블랙 프라이데이 같은 트래픽 폭증 상황에서 시스템을 보호하면서 유저에게 공정한 대기 경험을 제공하기 위해 Redis 기반 주문 대기열 시스템을 구축한다. 기존 주문 API(`POST /api/v1/orders`) 앞단에 대기열을 배치하여 Back-pressure를 구현하고, 입장 토큰과 실시간 순번 조회를 통해 유저 이탈을 방지한다.

### 스코프

Must-Have + Nice-To-Have 전체:

- Redis Sorted Set 기반 대기열
- 입장 토큰 발급 & 검증 (TTL 5분)
- 스케줄러 기반 순차 입장 처리 (100ms 간격, 분산 락)
- Polling 기반 순번 조회 (동적 주기 조절)
- SSE 기반 실시간 순번 Push
- Thundering Herd 완화 (발급 간격 분산)
- Graceful Degradation (Redis 장애 시 Fallback)

### 구현 우선순위

| Phase   | 범위           | 설명                                                           |
|---------|--------------|--------------------------------------------------------------|
| Phase 1 | Must-Have    | 대기열 진입/조회, 토큰 발급/검증, 스케줄러, Polling                           |
| Phase 2 | Nice-To-Have | SSE, 동적 Polling 주기, Thundering Herd 완화, Graceful Degradation |

> Phase 1을 먼저 완성한 뒤 Phase 2를 진행한다.

### 설계 결정 사항

| 결정          | 선택                     | 근거                                                      |
|-------------|------------------------|---------------------------------------------------------|
| 스케줄러 위치     | commerce-api           | 대기열은 주문 API 앞단의 관문. 분산 락으로 중복 실행 방지                     |
| 인증 방식       | 대기열 진입만 인증, 순번 조회는 경량화 | Polling 부하를 줄이기 위해 순번 조회는 userId 존재 확인만 수행              |
| 토큰 검증 위치    | HandlerInterceptor     | 주문 UseCase 수정 없이 관심사 분리. Graceful Degradation 시 비활성화 용이 |
| 토큰 검증+소비    | Lua 스크립트로 원자적 처리       | validate와 consume 분리 시 중복 주문 가능 → 원자 연산 필수              |
| Redis 읽기 전략 | Master-only template   | 순번 정확성과 토큰 검증 일관성이 중요. replica stale read 허용 불가         |
| Queue 도메인   | 독립 도메인                 | 별도 API, 별도 저장소(Redis), 별도 관심사(순번 관리)                    |

---

## Architecture

### 전체 흐름

```
[유저] → POST /api/v1/queue/enter (대기열 진입, 인증 필요)
      → Redis Sorted Set에 userId + timestamp 저장
      → 순번 + 예상 대기 시간 응답

[유저] → GET /api/v1/queue/position?userId={userId} (Polling, 인증 불필요)
      → 현재 순번 + 예상 대기 시간 + retryAfterMs 응답
      → (순번/대기인원은 근사치 — ZRANK + ZCARD는 별도 명령이므로 원자 스냅샷이 아님)

[유저] → GET /api/v1/queue/stream?userId={userId} (SSE)
      → 순번 변경/토큰 발급 시 서버가 Push

[스케줄러] → 100ms마다 실행 (분산 락 획득 후)
         → Lua 스크립트로 ZPOPMIN + 토큰 발급을 원자적 실행 (~14명)
         → SSE로 토큰 Push 이벤트 전송

[유저] → POST /api/v1/orders
         (헤더: X-Loopers-LoginId, X-Loopers-LoginPw, X-Idempotency-Key, X-Entry-Token)
      → EntryTokenInterceptor에서:
         1. X-Loopers-LoginId/LoginPw로 인증 → userId 획득
         2. Lua 스크립트로 토큰 검증+소비를 원자적 실행
         3. userId를 request attribute로 전달
      → 기존 OrderCreateUseCase 실행 (변경 없음)
      → R7 이벤트 파이프라인 동작
```

### 패키지 구조

```
com.loopers/
├── domain/queue/
│   ├── QueuePosition.kt          - VO (position, estimatedWaitSeconds)
│   ├── EntryToken.kt             - VO (token, userId)
│   ├── WaitingQueueRepository.kt - Port (대기열 조작)
│   └── EntryTokenRepository.kt   - Port (토큰 관리)
│
├── application/user/queue/
│   ├── QueueEnterUseCase.kt      - 대기열 진입
│   ├── QueuePositionUseCase.kt   - 순번 조회
│   ├── QueueCommand.kt           - 입력 DTO
│   └── QueueResult.kt            - 출력 DTO
│
├── infrastructure/queue/
│   ├── RedisWaitingQueueRepository.kt  - Sorted Set 구현 (master template)
│   ├── RedisEntryTokenRepository.kt    - String + TTL 구현 (master template, Lua 스크립트)
│   ├── QueueScheduler.kt              - 토큰 발급 스케줄러 (분산 락, Lua 스크립트)
│   ├── EntryTokenInterceptor.kt       - 인증 + 토큰 검증/소비 (Interceptor)
│   ├── EntryTokenWebMvcConfig.kt      - Interceptor 등록
│   └── QueueSseEmitterManager.kt      - SSE 연결 관리 (단일 인스턴스 전제)
│
├── interfaces/api/user/queue/
│   ├── UserQueueV1Controller.kt
│   ├── UserQueueV1ApiSpec.kt
│   ├── UserQueueV1Request.kt
│   └── UserQueueV1Response.kt
│
└── domain/common/
    └── ErrorType에 추가: ENTRY_TOKEN_REQUIRED, ENTRY_TOKEN_INVALID, QUEUE_SERVICE_UNAVAILABLE
```

---

## Components

### Domain Layer

#### QueuePosition (Value Object)

```kotlin
data class QueuePosition(
    val position: Long,              // 0-based 순번
    val estimatedWaitSeconds: Long,  // 예상 대기 시간 (초), 근사치
    val totalWaiting: Long,          // 전체 대기 인원, 근사치
)
```

> `position`과 `totalWaiting`은 `ZRANK` + `ZCARD` 별도 명령으로 조회하므로 원자 스냅샷이 아닌 **근사치**이다. 대기열 특성상 허용 가능.

#### EntryToken (Value Object)

```kotlin
data class EntryToken(
    val token: String,   // UUID
    val userId: Long,
)
```

#### WaitingQueueRepository (Port)

```kotlin
interface WaitingQueueRepository {
    fun enter(userId: Long): QueuePosition          // ZADD NX
    fun getPosition(userId: Long): QueuePosition?   // ZRANK + ZCARD
    fun size(): Long                                 // ZCARD
    fun remove(userId: Long)                         // ZREM
}
```

> `popFront`는 스케줄러의 Lua 스크립트가 직접 처리하므로 Port에서 제외.

#### EntryTokenRepository (Port)

```kotlin
interface EntryTokenRepository {
    fun issue(userId: Long): EntryToken                 // SET EX NX
    fun validateAndConsume(userId: Long, token: String): Boolean  // Lua: GET+비교+DEL 원자적
    fun exists(userId: Long): Boolean                   // EXISTS
}
```

> `validate()`와 `consume()`을 분리하지 않는다. `validateAndConsume()`으로 원자적 검증+소비를 보장한다.

### Application Layer

#### QueueEnterUseCase

- 입력: `QueueCommand.Enter(userId)`
- 동작:
    1. `entryTokenRepository.exists(userId)` → 이미 토큰이 있으면 토큰 정보 반환
    2. `waitingQueueRepository.enter(userId)` → 대기열 진입
- 출력: `QueueResult.Entered(position, estimatedWaitSeconds, totalWaiting)` 또는 `QueueResult.Ready(token)`

#### QueuePositionUseCase

- 입력: `QueueCommand.Position(userId)`
- 동작:
    1. `entryTokenRepository.exists(userId)` → 토큰 있으면 Ready 반환
    2. `waitingQueueRepository.getPosition(userId)` → 순번 조회
- 출력:
    - 대기 중: `QueueResult.Waiting(position, estimatedWaitSeconds, retryAfterMs, totalWaiting)`
    - 토큰 발급됨: `QueueResult.Ready(token)`
    - 대기열에 없음: `CoreException(QUEUE_ENTRY_NOT_FOUND)`

### Infrastructure Layer

#### RedisWaitingQueueRepository

- **RedisTemplate**: `@Qualifier(REDIS_TEMPLATE_MASTER)` — master-only
- Sorted Set 키: `queue:waiting`
- score: `System.currentTimeMillis().toDouble()` (진입 시각)
- member: `userId.toString()`
- 예상 대기 시간: `position / THROUGHPUT_PER_SECOND` (140 TPS 기준)

#### RedisEntryTokenRepository

- **RedisTemplate**: `@Qualifier(REDIS_TEMPLATE_MASTER)` — master-only
- 키 패턴: `queue:entry-token:{userId}`
- 값: UUID 문자열
- TTL: 300초 (5분)

**validateAndConsume Lua 스크립트** (원자적 검증+소비):

```lua
local stored = redis.call('GET', KEYS[1])
if stored == ARGV[1] then
    redis.call('DEL', KEYS[1])
    return 1
end
return 0
```

#### QueueScheduler

**스케줄러 설정**:

- `@Scheduled(fixedDelay = 100)` — 100ms 간격
- 배치 크기: 14명 (140 TPS / 10 intervals)

**분산 락**:

- 키: `queue:scheduler:lock`
- 획득: `SET queue:scheduler:lock {instanceId} PX 500 NX` (500ms lease)
- 해제: Lua 스크립트로 소유권 확인 후 DEL
  ```lua
  if redis.call('GET', KEYS[1]) == ARGV[1] then
      return redis.call('DEL', KEYS[1])
  end
  return 0
  ```
- 실패 시: skip (다음 100ms 주기에 재시도)
- 연장 없음: 배치가 500ms를 초과하면 자동 만료

**ZPOPMIN + 토큰 발급 Lua 스크립트** (원자적 pop+issue):

```lua
-- KEYS[1] = waiting-queue key
-- ARGV[1] = batch size
-- ARGV[2] = token TTL (seconds)
-- ARGV[3..N] = pre-generated UUIDs (호출 측에서 미리 생성)
local members = redis.call('ZPOPMIN', KEYS[1], ARGV[1])
local results = {}
local uuidIdx = 3
for i = 1, #members, 2 do
    local userId = members[i]
    local token = ARGV[uuidIdx]
    redis.call('SET', 'queue:entry-token:' .. userId, token, 'EX', ARGV[2])
    table.insert(results, userId)
    table.insert(results, token)
    uuidIdx = uuidIdx + 1
end
return results
```

> ZPOPMIN과 토큰 발급이 하나의 Lua 스크립트에서 실행되므로, pop 후 crash로 인한 유저 유실 문제가 해결된다. Redis 자체 crash 시에는 pop과 토큰 발급 모두 실행되지 않으므로 일관성 유지.

**토큰 만료 보충 전략**:

- 현재 설계: 매 배치마다 고정 14명을 pop하여 토큰 발급
- 만료된 토큰은 Redis TTL에 의해 자연 소멸 → 그만큼 시스템 처리 여유 자동 확보
- 추후 필요 시 "동시 활성 토큰 수 기반 발급"으로 확장 가능 (별도 카운터 + Keyspace Notification)

#### EntryTokenInterceptor

- `HandlerInterceptor` 구현
- `POST /api/v1/orders` 경로에만 적용 (`EntryTokenWebMvcConfig`에서 등록)

**동작 흐름**:

```
preHandle:
  1. X-Loopers-LoginId/LoginPw 헤더로 UserAuthenticateUseCase.authenticateAndGetId() 호출 → userId
  2. X-Entry-Token 헤더에서 토큰 추출
     - 헤더 없음 → CoreException(ENTRY_TOKEN_REQUIRED)
  3. entryTokenRepository.validateAndConsume(userId, token) 호출
     - false → CoreException(ENTRY_TOKEN_INVALID)
     - true → request.setAttribute("authenticatedUserId", userId) → 통과
  4. Redis 장애 시 → bypass (통과) + WARN 로깅 + 인메모리 rate limiter 체크
```

**에러 처리**:

- `CoreException`을 throw → 기존 `ApiControllerAdvice`가 `ApiResponse.fail()` 포맷으로 처리
- 신규 ErrorType 추가 필요:
    - `ENTRY_TOKEN_REQUIRED` (HTTP 400)
    - `ENTRY_TOKEN_INVALID` (HTTP 403)
    - `QUEUE_SERVICE_UNAVAILABLE` (HTTP 503)

**주문 컨트롤러 연동**:

- Interceptor가 인증을 수행하므로, 주문 컨트롤러에서 중복 인증을 방지하기 위해 request attribute에서 userId를 가져오는 방식 검토
- 최소 변경 원칙: 기존 컨트롤러는 그대로 두고 Interceptor의 인증은 토큰 검증용으로만 사용. DB 인증이 2번 호출되는 비효율이 있으나, 주문 API는 토큰이 있어야 진입하므로 트래픽이 이미 제어된 상태 (최대 140 TPS)

#### QueueSseEmitterManager

- `ConcurrentHashMap<Long, SseEmitter>` — userId별 emitter 관리
- `SseEmitter` timeout: 10분
- 이벤트 타입: `position` (순번 갱신), `enter` (토큰 발급)
- 연결 종료/타임아웃 시 자동 정리 (onCompletion, onTimeout 콜백)

**다중 인스턴스 제한사항**:

- 현재 설계는 **단일 인스턴스**를 전제한다.
- 다중 인스턴스 환경에서는 스케줄러가 실행된 인스턴스와 SSE 연결이 있는 인스턴스가 다를 수 있다.
- 해결책: Redis Pub/Sub을 backplane으로 사용하여 스케줄러가 토큰 발급 이벤트를 PUBLISH → 모든 인스턴스가 구독 → 해당 userId의 emitter가 있는 인스턴스에서 Push.
- 이 설계에서는 단일 인스턴스로 구현하고, 다중 인스턴스 대응은 추후 과제로 남긴다.

### API Layer

#### 엔드포인트

| Method | Path                     | 설명              | 인증                                |
|--------|--------------------------|-----------------|-----------------------------------|
| POST   | `/api/v1/queue/enter`    | 대기열 진입          | X-Loopers-LoginId/LoginPw (DB 인증) |
| GET    | `/api/v1/queue/position` | 순번 조회 (Polling) | userId 파라미터 (대기열 존재 확인만)          |
| GET    | `/api/v1/queue/stream`   | SSE 스트림         | userId 파라미터 (대기열 존재 확인만)          |

> 순번 조회는 Polling 빈도가 높으므로 DB 인증을 수행하지 않는다. userId가 대기열 Sorted Set에 존재하는지만 확인한다. 대기열에 없는 userId로 조회 시 에러 반환.

#### 주문 API 계약 (기존 + 신규)

| 헤더                | 필수 | 용도             |
|-------------------|----|----------------|
| X-Loopers-LoginId | Y  | 인증 (기존)        |
| X-Loopers-LoginPw | Y  | 인증 (기존)        |
| X-Idempotency-Key | Y  | 멱등성 보장 (기존)    |
| X-Entry-Token     | Y  | 대기열 입장 토큰 (신규) |

#### 응답 예시

**대기열 진입 (POST /api/v1/queue/enter)**

```json
{
  "meta": {
    "result": "SUCCESS"
  },
  "data": {
    "status": "WAITING",
    "position": 512,
    "estimatedWaitSeconds": 4,
    "totalWaiting": 1024
  }
}
```

**대기열 진입 — 이미 토큰 보유 (POST /api/v1/queue/enter)**

```json
{
  "meta": {
    "result": "SUCCESS"
  },
  "data": {
    "status": "READY",
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "tokenExpiresInSeconds": 300
  }
}
```

**순번 조회 — 대기 중 (GET /api/v1/queue/position)**

```json
{
  "meta": {
    "result": "SUCCESS"
  },
  "data": {
    "status": "WAITING",
    "position": 128,
    "estimatedWaitSeconds": 1,
    "totalWaiting": 640,
    "retryAfterMs": 1000
  }
}
```

**순번 조회 — 토큰 발급됨 (GET /api/v1/queue/position)**

```json
{
  "meta": {
    "result": "SUCCESS"
  },
  "data": {
    "status": "READY",
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "tokenExpiresInSeconds": 300
  }
}
```

---

## Redis Key Design

| 키                            | 자료구조       | 용도                                      | TTL       |
|------------------------------|------------|-----------------------------------------|-----------|
| `queue:waiting`              | Sorted Set | 대기열. score=timestamp(ms), member=userId | 없음        |
| `queue:entry-token:{userId}` | String     | 입장 토큰. value=UUID                       | 300초 (5분) |
| `queue:scheduler:lock`       | String     | 분산 락. value=instanceId                  | 500ms     |

> 모든 키에 `queue:` prefix를 사용하여 기존 Redis 키(`product:*`)와 namespace를 분리한다.

---

## Throughput Design

```
DB 커넥션 풀: 40 (실제 application.yml 설정값)
주문 1건 평균 처리 시간: 200ms (추정, 실측으로 조정 필요)
이론적 최대 TPS: 40 / 0.2 = 200 TPS
안전 마진 70%: 140 TPS
스케줄러 간격: 100ms (초당 10회)
배치 크기: 140 / 10 = 14명
```

> 200ms는 추정치이다. 실측 후 배치 크기를 조정해야 한다.

---

## Polling Dynamic Interval

| 순번 구간     | retryAfterMs | 근거               |
|-----------|--------------|------------------|
| 1~100     | 1,000ms      | 곧 입장. 빠른 피드백 필요  |
| 101~1,000 | 3,000ms      | 중간 대기. 적절한 빈도    |
| 1,001+    | 5,000ms      | 장기 대기. 서버 부하 최소화 |

---

## Graceful Degradation

| 기능                      | Redis 장애 시 동작                                    | 근거                |
|-------------------------|--------------------------------------------------|-------------------|
| 대기열 진입                  | `CoreException(QUEUE_SERVICE_UNAVAILABLE)` → 503 | 대기열 자체가 불가        |
| 순번 조회                   | `CoreException(QUEUE_SERVICE_UNAVAILABLE)` → 503 | 조회 불가             |
| SSE 스트림                 | 연결 종료 + error 이벤트                                | 유지 불가             |
| **토큰 검증 (Interceptor)** | **bypass → 통과 + 인메모리 rate limiter**              | 주문은 받되, 무제한 진입 방지 |

**토큰 bypass 시 인메모리 rate limiter**:

- `AtomicInteger` 기반 sliding window counter
- 초당 최대 140건 (= 설계 TPS) 초과 시 503 반환
- Redis 장애 상황에서 DB를 보호하는 최종 안전장치

구현: Redis 호출을 `try-catch`로 감싸고 `RedisConnectionFailureException` 발생 시 fallback. 모든 fallback 동작은 WARN 레벨로 로깅.

---

## Error Types (신규 추가)

| ErrorType                   | HTTP Status             | 설명                   |
|-----------------------------|-------------------------|----------------------|
| `ENTRY_TOKEN_REQUIRED`      | 400 Bad Request         | X-Entry-Token 헤더 누락  |
| `ENTRY_TOKEN_INVALID`       | 403 Forbidden           | 토큰이 유효하지 않거나 이미 사용됨  |
| `QUEUE_ENTRY_NOT_FOUND`     | 404 Not Found           | 대기열에 존재하지 않는 userId  |
| `QUEUE_SERVICE_UNAVAILABLE` | 503 Service Unavailable | Redis 장애로 대기열 서비스 불가 |

---

## Testing Strategy

### Unit Tests (Spring Context 없이)

- `QueueEnterUseCaseTest` — 진입, 중복 진입(ZADD NX), 이미 토큰 있는 경우
- `QueuePositionUseCaseTest` — 순번 조회, 토큰 발급 상태 조회, 미진입 유저 에러
- `QueueSchedulerTest` — 배치 크기만큼 pop+issue, 빈 대기열 시 skip, 분산 락 미획득 시 skip
- `EntryTokenInterceptorTest` — 유효 토큰(검증+소비), 무효 토큰, 헤더 누락, Redis 장애 bypass

### Integration Tests (Testcontainers Redis)

- `RedisWaitingQueueRepositoryIntegrationTest` — Sorted Set 조작, 순서 보장, 중복 방지(NX)
- `RedisEntryTokenRepositoryIntegrationTest` — 토큰 발급/검증+소비 원자성, TTL 만료 확인
- `QueueSchedulerIntegrationTest` — Lua 스크립트 실행, 분산 락 동작

### E2E Tests (SpringBootTest + MockMvc)

- `UserQueueV1ControllerE2ETest` — 대기열 진입 → 순번 조회 → 토큰 발급 후 조회 → 주문 API 호출 전체 흐름

### Concurrency Tests

- 동시 진입 100건 → 순서 보장 + 중복 방지 확인
- 스케줄러 동시 실행 → 분산 락으로 단일 실행 보장 확인
- 토큰 동시 사용 → validateAndConsume 원자성으로 단 1건만 성공 확인

---

## R7 Integration

대기열은 주문 API **앞단의 관문**이다. 주문 API 이후의 흐름은 변경 없음:

```
주문 완료 → ApplicationEvent(OrderCreatedEvent)
         → @TransactionalEventListener(AFTER_COMMIT)
         → KafkaOutboxEntity 저장
         → KafkaOutboxRelayService (매 1초) → Kafka 발행
         → commerce-streamer Consumer → Metrics 집계
```
