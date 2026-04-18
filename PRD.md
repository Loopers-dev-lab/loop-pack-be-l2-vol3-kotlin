# PRD: Redis 기반 주문 대기열 시스템

## 1. 개요

블랙프라이데이 같은 트래픽 폭증 시, 시스템이 처리할 수 있는 속도로 요청을 조절하는 **Back-pressure** 메커니즘을 구현한다.
Rate Limiting(거부) 대신 Queuing(대기)으로 유저에게 공정한 대기 경험을 제공하면서 시스템을 보호한다.
Round 7의 Kafka 이벤트 파이프라인(Outbox) 위에 주문 API **앞단에 대기열 관문**을 추가하는 구조.

### 전체 흐름

```
[유저] → POST /api/v1/queue/enter
      → Redis Sorted Set에 userId + timestamp 저장
      → 순번 응답 (e.g. 512번째)

[유저] → GET /api/v1/queue/position (Polling)
      → 현재 순번 + 예상 대기 시간 응답

[스케줄러] → 100ms마다 실행
         → ZPOPMIN으로 N명 꺼내기
         → 입장 토큰 발급 (Redis SET + TTL 5분)

[유저] → 순번 0 도달, 토큰 수신
      → POST /api/v1/orders (Header: X-Entry-Token)
      → 토큰 검증 → 주문 처리 → 토큰 삭제

[주문 이후] → R7 이벤트 파이프라인 동작
          → Outbox → Kafka → collector
```

## 2. 기술 스택

| 항목 | 스펙 |
|------|------|
| Language | Kotlin (JDK 21) |
| Framework | Spring Boot 3.4.4 |
| 대기열 | Redis Sorted Set |
| 입장 토큰 | Redis String + TTL |
| 스케줄러 | Spring @Scheduled |
| 순번 조회 | Polling 기반 (동적 주기) |

## 3. 설계 결정

| 결정 사항 | 선택 | 이유 |
|-----------|------|------|
| 토큰 검증 위치 | `OrderFacade.placeOrder()` 최상단 | Filter/Interceptor는 인증과 혼재됨, Facade에서 비즈니스 흐름과 함께 관리 |
| 도메인 구조 | 새 `queue` 패키지 | 대기열은 order와 별개 바운디드 컨텍스트 (자체 생명주기) |
| 스케줄러 설정 | `application.yml` 외부화 | 환경별 튜닝 가능 (`@Value`) |
| 토큰 형식 | UUID, `X-Entry-Token` 헤더 | 단순하고 예측 불가능, 기존 auth 헤더와 분리 |
| 토큰 읽기 | master 템플릿 사용 | 검증 시 replica lag으로 인한 false negative 방지 |

## 4. Redis 키 패턴

```
ZADD  order-queue  {timestamp}  {userId}     // 대기열 진입 (중복 방지: ZADD NX)
ZRANK order-queue  {userId}                  // 순번 조회 (0-based)
ZCARD order-queue                            // 전체 대기 인원
ZPOPMIN order-queue {N}                      // N명 꺼내기 (스케줄러)
SET   entry-token:{userId}  {token}  EX 300  // 토큰 발급 (5분 TTL)
GET   entry-token:{userId}                   // 토큰 검증
DEL   entry-token:{userId}                   // 토큰 소비
```

## 5. 처리량 설계 기준

```
DB 커넥션 풀: 50
주문 1건 평균 처리 시간: 200ms
→ 이론적 최대 TPS: 50 / 0.2 = 250 TPS
→ 안전 마진 70%: 175 TPS
→ 스케줄러: 100ms마다 ~18명씩 토큰 발급 (Thundering Herd 완화)
```

## 6. 파일 구조

### 신규 파일

**Domain Layer** (`domain/queue/`)
- `OrderQueueRepository.kt` — 대기열 인터페이스 (enqueue, getPosition, getTotalSize, popFront)
- `EntryTokenRepository.kt` — 토큰 인터페이스 (issue, get, consume)
- `OrderQueueService.kt` — 비즈니스 로직 (@Component)
- `QueuePosition.kt` — 순번 값 객체 (position, estimatedWaitSeconds, totalSize, token?)

**Application Layer** (`application/queue/`)
- `QueueFacade.kt` — 대기열 진입/순번 조회 오케스트레이션
- `QueuePositionInfo.kt` — 레이어 간 데이터 전달 Info DTO

**Infrastructure Layer** (`infrastructure/queue/`)
- `OrderQueueRedisRepository.kt` — Sorted Set 기반 구현 (master 쓰기, replica 읽기)
- `EntryTokenRedisRepository.kt` — String 기반 구현 (master only)
- `QueueAdmissionScheduler.kt` — @Scheduled, ZPOPMIN → 토큰 발급

**Interfaces Layer** (`interfaces/api/queue/`)
- `QueueController.kt` — POST /api/v1/queue/enter, GET /api/v1/queue/position
- `QueueApiSpec.kt` — Swagger 스펙
- `QueueDto.kt` — 요청/응답 DTO (EnterQueueResponse, QueuePositionResponse + pollingIntervalMs)

### 수정 파일

- `modules/redis/.../RedisKeys.kt` — `orderQueueKey()`, `entryTokenKey(userId)` 추가
- `interfaces/api/order/OrderController.kt` — `@RequestHeader("X-Entry-Token")` 파라미터 추가
- `interfaces/api/order/OrderApiSpec.kt` — placeOrder에 entryToken 파라미터 추가
- `application/order/OrderFacade.kt` — OrderQueueService 의존성 추가, `validateAndConsumeToken` 호출
- `application.yml` — 대기열 설정 추가

## 7. 레이어 의존성 (ArchUnit 준수)

```
interfaces/api/queue/QueueController
  → application/queue/QueueFacade
    → domain/queue/OrderQueueService
      → domain/queue/OrderQueueRepository (인터페이스)
      → domain/queue/EntryTokenRepository (인터페이스)

infrastructure/queue/OrderQueueRedisRepository implements OrderQueueRepository
infrastructure/queue/EntryTokenRedisRepository implements EntryTokenRepository
infrastructure/queue/QueueAdmissionScheduler → domain/queue/OrderQueueService

interfaces/api/order/OrderController (수정)
  → application/order/OrderFacade (수정)
    → domain/queue/OrderQueueService (신규 의존)
```

- `domain/queue/` → `support/error/`만 의존 (infrastructure, interfaces, application 의존 없음)
- `application/queue/` → `domain/queue/`만 의존
- `infrastructure/queue/` → `domain/queue/` + `modules/redis`

## 8. API 스펙

### 8.1 대기열 진입

```
POST /api/v1/queue/enter
Headers: X-Loopers-LoginId, X-Loopers-LoginPw

Response 200:
{
  "meta": { "result": "SUCCESS" },
  "data": {
    "position": 512,
    "estimatedWaitSeconds": 2,
    "totalSize": 1024
  }
}
```

### 8.2 순번 조회

```
GET /api/v1/queue/position
Headers: X-Loopers-LoginId, X-Loopers-LoginPw

Response 200 (대기 중):
{
  "meta": { "result": "SUCCESS" },
  "data": {
    "position": 128,
    "estimatedWaitSeconds": 1,
    "totalSize": 800,
    "token": null,
    "pollingIntervalMs": 3000
  }
}

Response 200 (입장 가능):
{
  "meta": { "result": "SUCCESS" },
  "data": {
    "position": 0,
    "estimatedWaitSeconds": 0,
    "totalSize": 800,
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "pollingIntervalMs": 0
  }
}
```

### 8.3 주문 (기존 API 수정)

```
POST /api/v1/orders
Headers: X-Loopers-LoginId, X-Loopers-LoginPw, Idempotency-Key, X-Entry-Token
```

### 8.4 Polling 동적 주기

| 순번 구간 | 주기 |
|-----------|------|
| ≤ 10 | 1초 |
| ≤ 50 | 2초 |
| ≤ 200 | 3초 |
| > 200 | 5초 |

## 9. 구현 단계

### Step 1 — Redis 기반 대기열

- [ ] `RedisKeys`에 `orderQueueKey()`, `entryTokenKey(userId)` 추가
- [ ] `OrderQueueRepository` 인터페이스 (domain)
- [ ] `EntryTokenRepository` 인터페이스 (domain)
- [ ] `QueuePosition` 데이터 클래스 (domain)
- [ ] `OrderQueueService` 구현 (domain)
- [ ] `OrderQueueRedisRepository` 구현 (infrastructure)
- [ ] `EntryTokenRedisRepository` 구현 (infrastructure)
- [ ] `OrderQueueServiceTest` 단위 테스트
- [ ] `OrderQueueRedisRepositoryTest` 통합 테스트
- [ ] `EntryTokenRedisRepositoryTest` 통합 테스트

### Step 2 — 입장 토큰 & 스케줄러

- [ ] `application.yml`에 대기열 설정 추가
- [ ] `QueueAdmissionScheduler` 구현
- [ ] `OrderFacade.placeOrder()`에 `entryToken` 파라미터 추가 + `validateAndConsumeToken` 호출
- [ ] `OrderController.placeOrder()`에 `@RequestHeader("X-Entry-Token")` 추가
- [ ] `OrderApiSpec` 업데이트

### Step 3 — Polling 기반 실시간 순번 조회

- [ ] `QueuePositionInfo` (application)
- [ ] `QueueFacade` 구현
- [ ] `QueueApiSpec` Swagger 정의
- [ ] `QueueDto` — EnterQueueResponse, QueuePositionResponse
- [ ] `QueueController` 구현
- [ ] 예상 대기 시간 계산: `position / 175.0` (초)
- [ ] 동적 Polling 주기 응답
- [ ] 토큰 발급 시 position=0, token 포함 응답
- [ ] `QueueFacadeTest` 단위 테스트
- [ ] `QueueFacadeIntegrationTest` 통합 테스트
- [ ] `QueueApiE2ETest` E2E 테스트
- [ ] 기존 `OrderApiE2ETest` 수정

## 10. 검증

- [ ] `./gradlew :apps:commerce-api:test` — ArchUnit 포함 전체 테스트 통과
- [ ] 동시 진입 테스트: 같은 userId로 여러 번 enqueue → 1번만 등록
- [ ] 토큰 만료 테스트: TTL 후 GET 시 null
- [ ] 전체 흐름 E2E: 진입 → 순번 조회 → 스케줄러 토큰 발급 → 토큰으로 주문
- [ ] 토큰 없이/잘못된 토큰으로 주문 → FORBIDDEN 에러
- [ ] `http/queue-v1.http` 파일 작성

## 11. Round 7과의 연결점

| R7에서 배운 것 | R8에서 활용하는 것 |
|---------------|-------------------|
| 주문 → 이벤트 발행 (ApplicationEvent) | 주문 처리 후 후속 이벤트는 그대로 이벤트 기반 |
| Kafka 파이프라인 | 주문 완료 이벤트 → Kafka → collector |
| Outbox Pattern | 주문 이벤트 발행의 신뢰성 보장 |

대기열은 **주문 API 앞단의 관문**이고, 주문 API 이후의 흐름은 **R7에서 구축한 이벤트 파이프라인**이 그대로 동작한다.

## 12. SSE 기반 실시간 순번 Push

### 12.1 배경

현재 Polling 방식은 클라이언트가 주기적으로 GET /api/v1/queue/position을 호출한다.
대기열이 길어질수록 불필요한 요청이 누적되고, 순번 변화가 없어도 응답을 반환해야 한다.
SSE(Server-Sent Events)를 도입하면 서버가 순번 변화 시에만 Push하여 **네트워크 비용 절감 + 실시간성 향상**을 달성한다.

### 12.2 설계 결정

| 결정 사항 | 선택 | 이유 |
|-----------|------|------|
| SSE 구현 방식 | Spring MVC `SseEmitter` | 기존 servlet 기반 유지, WebFlux 마이그레이션 불필요 |
| Polling과 병행 | 기존 Polling API 유지 + SSE 엔드포인트 추가 | 클라이언트 호환성 보장, 점진적 전환 |
| 연결 타임아웃 | 5분 (토큰 TTL과 동일) | 대기열 이탈 유저의 연결 자동 정리 |
| Push 트리거 | 스케줄러가 admitUsers 실행 후 전체 연결에 브로드캐스트 | 순번 변화가 발생하는 시점에만 Push |

### 12.3 API 스펙

```
GET /api/v1/queue/stream
Headers: X-Loopers-LoginId, X-Loopers-LoginPw
Accept: text/event-stream

--- SSE 이벤트 ---
event: position
data: {"position": 128, "estimatedWaitSeconds": 0.73, "totalSize": 800, "token": null}

event: position
data: {"position": 52, "estimatedWaitSeconds": 0.30, "totalSize": 750, "token": null}

event: admitted
data: {"position": 0, "estimatedWaitSeconds": 0, "totalSize": 700, "token": "550e8400-..."}
```

### 12.4 파일 구조

**신규 파일**

| 파일 | 역할 |
|------|------|
| `domain/queue/QueueEventPublisher.kt` | 순번 변화 이벤트 발행 인터페이스 |
| `infrastructure/queue/SseEmitterRepository.kt` | userId → SseEmitter 매핑 관리 (ConcurrentHashMap) |
| `interfaces/api/queue/QueueStreamController.kt` | GET /api/v1/queue/stream 엔드포인트 |
| `interfaces/api/queue/QueueStreamApiSpec.kt` | Swagger 스펙 |

**수정 파일**

| 파일 | 변경 내용 |
|------|-----------|
| `application/queue/QueueAdmissionScheduler.kt` | admitUsers 후 SseEmitterRepository를 통해 브로드캐스트 |

### 12.5 주의사항

- SseEmitter는 서버 메모리에 유저별 연결을 유지하므로, **대기 인원이 수만 명**이면 메모리 부담 발생
- 멀티 인스턴스 배포 시 Redis Pub/Sub로 인스턴스 간 이벤트 전파 필요
- 클라이언트 연결 끊김(onCompletion, onTimeout, onError) 시 SseEmitter 정리 필수

### 12.6 구현 단계

- [ ] `SseEmitterRepository` 구현 (ConcurrentHashMap, 연결 생명주기 관리)
- [ ] `QueueStreamController` — GET /api/v1/queue/stream 엔드포인트
- [ ] `QueueAdmissionScheduler`에서 admit 후 연결된 유저에게 순번 Push
- [ ] 토큰 발급 시 `admitted` 이벤트 전송 후 SseEmitter 완료 처리
- [ ] 연결 타임아웃/에러 시 자동 정리
- [ ] SSE 연결/해제 통합 테스트

## 13. Thundering Herd 완화

### 13.1 배경

현재 스케줄러는 **100ms마다 정확히 18명**에게 토큰을 발급한다.
이 18명이 토큰을 받는 즉시 동시에 주문 API를 호출하면, 순간적으로 18개 요청이 몰리는 **Thundering Herd** 현상이 발생한다.
DB 커넥션 풀(50)에 비해 크지 않지만, 발급 간격에 Jitter를 추가하면 요청을 시간축으로 분산시킬 수 있다.

### 13.2 현재 상태

```
[100ms] → 18명 동시 토큰 발급 → 18명 동시 주문 요청 (burst)
[200ms] → 18명 동시 토큰 발급 → 18명 동시 주문 요청 (burst)
```

### 13.3 개선 전략

#### 전략 A — 토큰 TTL Jitter

토큰 발급 시 TTL에 랜덤 오프셋(0~2초)을 추가하여, 클라이언트가 토큰을 인지하는 시점을 분산시킨다.
단, 이 방식은 클라이언트가 Polling으로 토큰을 발견하는 시점에 의존하므로 효과가 제한적이다.

#### 전략 B — 배치 분할 발급 (권장)

한 번에 18명을 발급하는 대신, **배치를 소분할**하여 시간 간격을 둔다.

```
현재: 100ms마다 18명 일괄
개선: 50ms마다 9명 발급 → 동일 TPS, burst 절반
```

| 설정 | 현재 | 개선 |
|------|------|------|
| fixed-rate | 100ms | 50ms |
| batch-size | 18 | 9 |
| 초당 발급 | 180명 | 180명 (동일) |
| burst 크기 | 18 | 9 (50% 감소) |

#### 전략 C — 랜덤 Jitter 스케줄링

`@Scheduled` 대신 `ScheduledExecutorService`를 사용하여 실행 간격에 ±20% 랜덤 Jitter를 적용한다.

```
기본 간격: 100ms
실제 실행: 80ms ~ 120ms (랜덤)
→ 주기적 burst 패턴이 깨져 부하가 자연스럽게 분산
```

### 13.4 설계 결정

| 결정 사항 | 선택 | 이유 |
|-----------|------|------|
| 완화 전략 | 전략 B (배치 분할) + 전략 C (Jitter) 조합 | 단순하면서 효과적, 기존 구조 변경 최소화 |
| Jitter 구현 | `ScheduledExecutorService` + 랜덤 delay | `@Scheduled`는 고정 간격만 지원 |
| 설정 외부화 | `application.yml`에 jitter-range 추가 | 환경별 튜닝 가능 |

### 13.5 설정

```yaml
queue:
  admission:
    batch-size: 9              # 기존 18 → 9로 축소
    fixed-rate: 50             # 기존 100ms → 50ms
    jitter-range: 20           # ±20ms 랜덤 Jitter
```

### 13.6 파일 구조

**수정 파일**

| 파일 | 변경 내용 |
|------|-----------|
| `application/queue/QueueAdmissionScheduler.kt` | `@Scheduled` → `ScheduledExecutorService` + Jitter |
| `application.yml` | batch-size: 9, fixed-rate: 50, jitter-range: 20 추가 |

### 13.7 구현 단계

- [ ] `QueueAdmissionScheduler`를 `ScheduledExecutorService` 기반으로 리팩터링
- [ ] Jitter 로직 구현 (baseRate ± jitterRange 범위의 랜덤 delay)
- [ ] `application.yml` 설정 변경 (batch-size: 9, fixed-rate: 50, jitter-range: 20)
- [ ] 기존 `QueueAdmissionSchedulerTest` 수정
- [ ] Jitter 범위 내 실행 검증 테스트

## 14. Graceful Degradation (Redis 장애 시 Fallback)

### 14.1 배경

대기열 시스템은 Redis에 전적으로 의존한다.
Redis 장애 시 대기열 진입, 순번 조회, 토큰 발급 모두 실패하여 **주문 자체가 불가능**해진다.
PG 결제에 이미 적용된 Resilience4j 패턴을 대기열에도 적용하여, Redis 장애 시에도 제한적으로 주문을 허용하는 Fallback 전략을 구현한다.

### 14.2 장애 시나리오별 전략

| 시나리오 | 현재 동작 | 개선 후 동작 |
|----------|----------|-------------|
| Redis 일시 장애 (수초) | 500 에러 | Retry (3회, 지수 백오프) 후 복구 |
| Redis 지속 장애 (수분) | 500 에러 지속 | Circuit Breaker OPEN → Fallback: 대기열 없이 주문 허용 |
| Redis replica 장애 | 읽기 실패 | Master로 읽기 폴백 (기존 REPLICA_PREFERRED 동작) |

### 14.3 설계 결정

| 결정 사항 | 선택 | 이유 |
|-----------|------|------|
| Resilience4j 적용 계층 | Infrastructure (Repository 구현체) | 도메인 계층은 인프라 기술에 무관하게 유지 |
| Fallback 전략 | 대기열 우회 (bypass) | 장애 시에도 주문 가능, 매출 손실 방지 |
| Circuit Breaker 대상 | OrderQueueRedisRepository, EntryTokenRedisRepository | Redis 호출 지점에 직접 적용 |
| 토큰 검증 Fallback | bypass 모드에서 토큰 검증 스킵 | 대기열이 작동하지 않으면 토큰도 발급되지 않으므로 |

### 14.4 Resilience4j 설정

```yaml
resilience4j:
  circuitbreaker:
    instances:
      order-queue:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 3
  retry:
    instances:
      order-queue:
        max-attempts: 3
        wait-duration: 200ms
        retry-exceptions:
          - org.springframework.data.redis.RedisConnectionFailureException
```

### 14.5 파일 구조

**신규 파일**

| 파일 | 역할 |
|------|------|
| `infrastructure/queue/OrderQueueFallbackRepository.kt` | CircuitBreaker OPEN 시 bypass 동작 구현 |
| `infrastructure/queue/EntryTokenFallbackRepository.kt` | CircuitBreaker OPEN 시 bypass 동작 구현 |

**수정 파일**

| 파일 | 변경 내용 |
|------|-----------|
| `infrastructure/queue/OrderQueueRedisRepository.kt` | `@CircuitBreaker`, `@Retry` 어노테이션 추가, fallbackMethod 지정 |
| `infrastructure/queue/EntryTokenRedisRepository.kt` | `@CircuitBreaker`, `@Retry` 어노테이션 추가, fallbackMethod 지정 |
| `domain/queue/OrderQueueService.kt` | bypass 모드 감지 시 토큰 검증 스킵 로직 |
| `application.yml` | resilience4j order-queue 인스턴스 설정 추가 |

### 14.6 Fallback 동작 상세

```
[정상 상태]
enqueue → Redis ZADD → 순번 반환
validateToken → Redis GET → 검증

[Circuit Breaker OPEN]
enqueue → fallback → 즉시 성공 반환 (bypass)
validateToken → fallback → 검증 스킵 (bypass)
→ 대기열 없이 직접 주문 허용
```

### 14.7 주의사항

- bypass 모드에서는 **처리량 제어가 해제**되므로, DB 커넥션 풀 고갈 위험이 있다
- bypass 모드 진입/해제 시 **모니터링 알림** 필수 (로그 + 메트릭)
- Circuit Breaker가 HALF_OPEN으로 전환 시 Redis 복구를 자동 감지하여 정상 모드로 복귀

### 14.8 구현 단계

- [ ] `OrderQueueRedisRepository`에 `@CircuitBreaker`, `@Retry` 적용 + fallbackMethod 구현
- [ ] `EntryTokenRedisRepository`에 `@CircuitBreaker`, `@Retry` 적용 + fallbackMethod 구현
- [ ] `OrderQueueService.validateAndConsumeToken`에 bypass 모드 처리 추가
- [ ] `application.yml`에 resilience4j order-queue 설정 추가
- [ ] bypass 모드 진입 시 WARN 로그 출력
- [ ] Redis 장애 시나리오 통합 테스트 (TestContainers Redis stop/start)
