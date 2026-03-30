# Round 8 — Redis 기반 대기열 시스템

## 개요

Redis Sorted Set 기반 대기열로 트래픽 폭증 시 Back-pressure를 적용한다.
대기열 진입 → 스케줄러 배치 입장 토큰 발급 → 토큰 보유 시만 주문 허용.

## 영향 범위

### 신규 파일
- `domain/queue/waiting/` — WaitingQueue 도메인 모델, Repository 인터페이스
- `domain/queue/token/` — EntryToken 도메인 모델, Repository 인터페이스
- `infrastructure/queue/` — RedisWaitingQueueRepository, RedisEntryTokenRepository
- `application/queue/` — EnterQueueUseCase, GetQueuePositionUseCase, IssueEntryTokensUseCase
- `interfaces/api/queue/` — QueueV1Controller, QueueV1ApiSpec, dto
- `interfaces/support/interceptor/EntryTokenInterceptor`
- `interfaces/support/scheduler/QueueScheduler`

### 수정 파일
- `WebMvcConfig` — EntryTokenInterceptor 등록 (AuthInterceptor 이후 순서)
- `PlaceOrderUseCase` — 주문 완료 후 토큰 삭제

### 관련 기존 파일
- `modules/redis/RedisConfig.kt` — REDIS_TEMPLATE_MASTER qualifier 재사용
- `interfaces/support/interceptor/AuthInterceptor.kt` — 인터셉터 패턴 참고
- `interfaces/support/scheduler/OutboxRelayScheduler.kt` — 스케줄러 패턴 참고

---

## 구현 계획

> **병렬 그룹 범례**: 같은 `[P-X]` 태그 항목은 독립적으로 동시 진행 가능.
> 특히 **[P-DOM] A+B ‖ F+G**는 두 aggregate가 독립적이라 Step 1/2를 넘어 동시 작업 가능.

### Step 0 — 선행 조건 (인프라 준비)

- [x] 0-1: ErrorType에 `FORBIDDEN(403)`, `TOO_MANY_REQUESTS(429)` 추가
- [x] 0-2: Constants.kt에 `HEADER_ENTRY_TOKEN = "X-Entry-Token"` 상수 추가
- [x] 0-3: application.yml에 queue 설정 프로퍼티 추가 (`queue.batch-size`, `queue.max-capacity`, `queue.token-ttl-seconds`, `queue.throughput-tps`, `queue.scheduler-delay-ms`)
- [x] 0-4: QueueProperties 설정 클래스 생성 (@ConfigurationProperties)

--- checkpoint: lint + test (선행 조건) ---

### Step 1 — Redis 기반 대기열

#### A. WaitingQueue 도메인 모델

- [x] [P-DOM] 1-A-1: [RED] userId + score(timestamp)로 대기열 진입 → [GREEN] WaitingQueue 도메인 모델 구현
- [x] [P-DOM] 1-A-2: [RED] 대기열 최대 인원 상한(50,000) 초과 시 예외 → [GREEN] 상한 검증 로직 (Lua 스크립트)
- [x] [P-DOM] 1-A-3: [RED] 순번(position)과 예상 대기 시간 계산 (순번 / 초당 처리량) → [GREEN] QueuePosition.of()

#### B. WaitingQueue Repository

- [x] [P-DOM] 1-B-1: FakeWaitingQueueRepository 구현 (단위 테스트용)
- [x] [P-DOM] 1-B-2: [RED] enter — userId 중복 시 기존 순번 유지 → [GREEN] WaitingQueueRepository 인터페이스 + RedisWaitingQueueRepository 구현 (ZADD + ZCARD 원자성 보장: Lua 스크립트로 상한 검증 + 삽입 atomic 수행)
- [x] [P-DOM] 1-B-3: [RED] findPosition — ZRANK 기반 순번 조회, 없으면 null → [GREEN] 구현
- [x] [P-DOM] 1-B-4: [RED] count — ZCARD 전체 대기 인원 → [GREEN] 구현
- [x] [P-DOM] 1-B-5: [RED] popMin(N) — ZPOPMIN으로 N명 꺼내기 → [GREEN] 구현

--- checkpoint: lint + test (도메인 + 저장소 — [P-DOM] 완료 후) ---

#### C. Application DTO + EnterQueueUseCase

> **의존성 주의**: EnterQueueUseCase는 EntryTokenRepository에도 의존 (토큰 보유 여부 확인).
> [P-DOM] 완료 후 시작해야 함.

- [x] [P-UC] 1-C-0: QueuePositionInfo (position, estimatedWaitSeconds, token?) Application DTO 정의
- [x] [P-UC] 1-C-1: [RED] 이미 토큰 보유 중이면 토큰 정보 반환 → [GREEN] EnterQueueUseCase 구현 (EntryTokenRepository 의존)
- [x] [P-UC] 1-C-2: [RED] 이미 대기열에 있으면 기존 순번 반환 → [GREEN] 구현
- [x] [P-UC] 1-C-3: [RED] 상한 초과 시 429 예외 → [GREEN] 구현
- [x] [P-UC] 1-C-4: [RED] 신규 진입 시 순번 + 예상 대기 시간 반환 → [GREEN] 구현

#### D. Application — GetQueuePositionUseCase

- [x] [P-UC] 1-D-1: [RED] 대기열에 없는 유저 조회 시 404 예외 → [GREEN] GetQueuePositionUseCase 구현
- [x] [P-UC] 1-D-2: [RED] 토큰 보유 시 응답에 토큰 포함 → [GREEN] 구현 (EntryTokenRepository.find 호출)
- [x] [P-UC] 1-D-3: [RED] 정상 조회 시 순번 + 예상 대기 시간 반환 → [GREEN] 구현

--- checkpoint: lint + test (Step 1 UseCase) ---

#### E. API — POST /queue/enter, GET /queue/position

- [x] 1-E-1: [RED] POST /queue/enter — 진입 응답(순번, 예상 대기 시간) → [GREEN] QueueV1Controller + QueueV1ApiSpec + dto
- [x] 1-E-2: [RED] GET /queue/position — 순번 조회 응답 (토큰 포함 케이스) → [GREEN] 구현
- [x] 1-E-3: WebMvcConfig에 `/api/v1/queue/**` 경로를 authInterceptor에 등록

--- checkpoint: lint + test (Step 1 전체) ---

---

### Step 2 — 입장 토큰 & 스케줄러

#### F. EntryToken 도메인 모델

- [x] [P-DOM] 2-F-1: [RED] 토큰 값 생성 (UUID) → [GREEN] EntryToken 도메인 모델
- [x] [P-DOM] 2-F-2: [RED] 토큰 TTL = 300초 상수 정의 → [GREEN] EntryToken.defaultTtlSeconds()

#### G. EntryToken Repository

- [x] [P-DOM] 2-G-1: FakeEntryTokenRepository 구현
- [x] [P-DOM] 2-G-2: [RED] issue — SET entry-token:{userId} {token} EX 300 → [GREEN] EntryTokenRepository 인터페이스 + RedisEntryTokenRepository 구현
- [x] [P-DOM] 2-G-3: [RED] find — GET entry-token:{userId}, 없으면 null → [GREEN] 구현
- [x] [P-DOM] 2-G-4: [RED] delete — DEL entry-token:{userId} → [GREEN] 구현

--- checkpoint: lint + test (도메인 + 저장소 — [P-DOM] 완료 후) ---

#### H. Application — IssueEntryTokensUseCase

- [ ] [P-S2] 2-H-1: [RED] popMin으로 N명 꺼내 각각 토큰 발급 → [GREEN] IssueEntryTokensUseCase 구현
- [ ] [P-S2] 2-H-2: [RED] 대기열이 비어있으면 아무 동작 안 함 → [GREEN] 구현
- [ ] [P-S2] 2-H-3: [RED] 배치 크기(batchSize) 설정값으로 주입 → [GREEN] 구현

#### I. QueueScheduler

- [ ] [P-S2] 2-I-1: [RED] 100ms 고정 딜레이로 IssueEntryTokensUseCase 호출 → [GREEN] QueueScheduler 구현

--- checkpoint: lint + test (Step 2 스케줄러) ---

#### J. EntryTokenInterceptor + 주문 연동

- [ ] [P-S2] 2-J-1: [RED] X-Entry-Token 헤더 없으면 403 → [GREEN] EntryTokenInterceptor 구현
- [ ] [P-S2] 2-J-2: [RED] 토큰 불일치(다른 userId) 시 403 → [GREEN] 구현
- [ ] [P-S2] 2-J-3: [RED] 토큰 만료(Redis에 없음) 시 403 → [GREEN] 구현
- [ ] [P-S2] 2-J-4: WebMvcConfig에 EntryTokenInterceptor 등록 (AuthInterceptor addInterceptor 호출 이후에 배치 → 순서 보장, `/api/v1/orders/**`)
- [ ] [P-S2] 2-J-5: [RED] 주문 완료 후 토큰 삭제 → [GREEN] PlaceOrderUseCase 수정
- [ ] 2-J-6: SwaggerConfig에 `X-Entry-Token` SecurityScheme 등록

--- checkpoint: lint + test (Step 2 전체) ---

---

### Step 3 — 통합 시나리오 검증

#### K. 통합 테스트

- [ ] 3-K-1: [RED] 동시 진입 테스트 — 100명 동시 진입 시 순번이 중복 없이 보장된다
- [ ] 3-K-2: [RED] 토큰 만료 테스트 — TTL 이후 토큰으로 주문 시도 시 403
- [ ] 3-K-3: [RED] 대기열 상한 초과 테스트 — 50,000 초과 진입 시 429
- [ ] 3-K-4: [RED] 대기열 없이 주문 시도 — 토큰 없이 POST /orders 시 403
- [ ] 3-K-5: [RED] 정상 플로우 E2E — 진입 → 스케줄러 실행 → 토큰 발급 → 주문 성공
- [ ] 3-K-6: [RED] 처리량 초과 테스트 — 배치 크기 이상 동시 요청 시 시스템 안정성 확인
- [ ] 3-K-7: [RED] 토큰 만료 후 재진입 시 맨 뒤 순번 배정 확인

--- checkpoint: lint + test (Step 3 통합) ---

---

### Step 4 — 성능 최적화 (Thundering Herd + Polling 동적 주기)

#### L. Thundering Herd 완화 — Jitter

- [ ] [P-OPT] 4-L-1: [RED] 스케줄러 배치 내 발급 간격에 랜덤 jitter 추가 (0~50ms) → [GREEN] IssueEntryTokensUseCase 수정
- [ ] [P-OPT] 4-L-2: [RED] jitter 범위가 설정값으로 주입 가능 → [GREEN] 구현

#### M. Polling 동적 주기 조절

- [ ] [P-OPT] 4-M-1: [RED] 순번 > 1000이면 pollIntervalMs = 5000, > 100이면 3000, 이하 1000 → [GREEN] 계산 로직
- [ ] [P-OPT] 4-M-2: [RED] 순번 조회 응답에 recommendedPollIntervalMs 필드 포함 → [GREEN] dto + UseCase 수정

--- checkpoint: lint + test (Step 4 성능 최적화) ---

---

### Step 5 — SSE 기반 실시간 순번 Push

#### N. SSE Endpoint

- [ ] 5-N-1: [RED] GET /queue/events — SseEmitter 생성 + 순번 변경 시 push → [GREEN] QueueV1Controller에 SSE 엔드포인트 추가 (기존 컨트롤러 활용)
- [ ] 5-N-2: [RED] 토큰 발급 시 SSE 이벤트로 토큰 정보 push → [GREEN] 구현
- [ ] 5-N-3: [RED] 연결 해제(timeout/error) 시 emitter 정리 → [GREEN] 구현
- [ ] 5-N-4: WebMvcConfig에 SSE 경로 등록 (인증 필수)

--- checkpoint: lint + test (Step 5 SSE) ---

---

### Step 6 — Graceful Degradation (Redis 장애 Fallback)

#### O. Redis 장애 감지 + Fallback

- [ ] 6-O-1: [RED] Redis 연결 실패 시 대기열/토큰 검증을 건너뛰고 주문 허용 → [GREEN] Fallback 로직 구현
- [ ] 6-O-2: [RED] Fallback 진입/복구 시 로그 경고 출력 → [GREEN] 구현
- [ ] 6-O-3: [RED] Redis 복구 시 자동으로 정상 모드 전환 → [GREEN] 구현

--- checkpoint: lint + test (Step 6 Graceful Degradation) ---

---

## 고려사항

- **패키지 구조**: `domain/queue/` 바운디드 컨텍스트 안에 `waiting/`, `token/` aggregate 배치 (catalog 패턴)
- **스케줄러 배치 크기**: `batchSize = 18` (175 TPS / 10회/초). 설정 파일에서 주입 (`@Value`)
- **대기열 상한**: `MAX_CAPACITY = 50_000` (총 사용자 10만 기준, 대기 10분 이내)
- **토큰 TTL**: 300초 상수. 추후 설정값으로 외부화 가능
- **EntryTokenInterceptor**: AuthInterceptor 이후 실행 (userId가 request attribute에 세팅된 후)
- **토큰 전달 방식**: `X-Entry-Token` 헤더 (Request Body 불변 원칙)
- **SSE vs Polling**: Step 1~3에서 Polling 기반 구현 완료 후, Step 5에서 SSE 추가. 공존 가능
- **Graceful Degradation**: Redis 장애 시 대기열 없이 통과 허용 (주문 가능성 > 서비스 중단)
- **스케줄러 스레드 풀**: fixedDelay=100ms로 기존 스케줄러(5000ms, 60000ms)보다 50배 빠름. Spring TaskScheduler 기본 풀 크기(1)에서 병목 가능 → ThreadPoolTaskScheduler 설정 검토 필요
- **Lua 스크립트**: RedisWaitingQueueRepository.enter()에서 ZCARD 확인 + ZADD를 원자적으로 수행하여 상한 초과 race condition 방지
