# 07. 대기열 시스템 PRD

---

## 1. 배경 및 목적

블랙 프라이데이 행사를 앞두고, 주문 API 앞단에 **대기열 시스템**을 구축한다.
DB 커넥션 풀(50개)을 보호하면서 유저에게 공정한 진입 순서를 보장하고,
대규모 트래픽 상황에서도 시스템 안정성을 유지하는 것이 목표다.

### 핵심 문제

- 블프 트래픽(수천~수만 동시 접속)이 주문 API에 직접 유입되면 DB 커넥션 풀 고갈
- 선착순 공정성 없이 요청이 몰리면 일부 유저만 반복 성공하는 불공정 발생
- 서버 과부하로 전체 서비스 장애 확산 위험

### 해결 방향

놀이공원식 **가상 대기열**을 채택한다.
유저는 번호표를 받고 자유롭게 대기하며, 순서가 되면 입장 토큰을 발급받아 주문한다.

---

## 2. 용어 정의

| 한글 | 영문 | 설명 |
|------|------|------|
| 대기열 | Waiting Queue | Redis Sorted Set 기반 진입 대기 공간 |
| 순번 | Position | 대기열 내 유저의 현재 위치 (1-based) |
| 입장 토큰 | Entry Token | 주문 API 진입 권한을 증명하는 UUID (TTL 5분) |
| 배치 크기 | Batch Size | 스케줄러 1회 실행 시 대기열에서 꺼내는 인원 수 |
| 토글 | Toggle | 대기열 활성/비활성 전환 (Redis flag) |

---

## 3. 대기열 방식 선택: 놀이공원식 vs 은행창구식

| 구분 | 은행창구식 | 놀이공원식 (채택) |
|------|-----------|-----------------|
| 대기 방식 | 연결 유지하며 대기 (blocking) | 번호표 받고 자유롭게 대기 (non-blocking) |
| 순번 확인 | 줄에 서있으면 자동으로 이동 | Polling으로 내 순번 확인 |
| 입장 | 순서 되면 바로 서비스 | 토큰 발급 → 토큰으로 입장 |
| 이탈 | 줄에서 빠지면 순서 상실 | TTL 내 토큰 미사용 시 자연 만료 |
| 서버 부하 | 대기 중 커넥션 점유 | 커넥션 점유 없음 (stateless) |

### 채택 근거

- 수만 명 동시 대기 시 은행창구식은 커넥션 점유로 서버 자원 고갈
- Polling 기반 stateless → 서버 재시작/스케일아웃 무영향
- 토큰 TTL로 no-show 자동 처리 (별도 타임아웃 관리 불필요)
- 유저가 대기 중 다른 페이지 탐색 가능 → UX 우위

---

## 4. 주요 의사결정 및 근거

### D1. `queue` 독립 도메인

| 선택지 | 판단 |
|--------|------|
| A) `queue` 독립 도메인 | **채택** |
| B) `order` 도메인 하위 | 기각 |

**근거**: 대기열은 "주문을 보호하는 관문"이지 주문 자체가 아니다. fcfscoupon 패턴과 일관성 유지. 향후 주문 외 다른 API(한정판 구매 등)에도 재사용 가능. order 패키지에 Redis 의존 추가 시 관심사 혼재.

### D2. commerce-api `@Scheduled` + Redis 분산 락

| 선택지 | 판단 |
|--------|------|
| A) commerce-api `@Scheduled` (단일 인스턴스) | 멀티 인스턴스 중복 실행 |
| B) commerce-batch Job | 1~3초 실시간성에 부적합 |
| **C) commerce-api `@Scheduled` + Redis SET NX EX** | **채택** |

**근거**: 스케줄러 주기 3초는 배치 철학(분 단위)과 맞지 않음. `SET NX EX` 한 줄로 멀티 인스턴스 중복 방지. Lua 스크립트로 락 소유자만 해제하여 안전성 확보. PaymentPollingScheduler와 동일 패턴.

### D3. Redis flag 토글 (수동 ON/OFF)

| 선택지 | 판단 |
|--------|------|
| A) 항상 ON | 평시 불필요한 대기열 경유 → latency 추가 |
| **B) Redis flag 토글** | **채택** |
| C) 자동 토글 (임계값 기반) | 오버엔지니어링, flapping 위험 |

**근거**: 블프는 예정된 이벤트 → 수동 토글이 안전. 어드민 API(`POST /api-admin/v1/queue/toggle`)로 즉시 전환. 대기열 비활성 시 토큰 검증 인터셉터 자동 통과 (zero overhead).

### D4. fail-closed 토큰 검증 (Graceful Degradation)

| 선택지 | 판단 |
|--------|------|
| A) 우회 허용 (가용성 우선) | 대기열 무력화, DB 과부하 위험 |
| B) 주문 거부 (정합성 우선) | Redis = SPoF, 매출 손실 |
| **C) fail-closed (503) + 어드민 수동 우회** | **채택** |

**근거**: 대기열의 존재 이유가 DB 보호이므로, Redis 장애 시 DB를 노출하는 것은 본말전도. QueueTokenInterceptor가 Redis 예외 시 503 반환. 어드민 `POST /toggle`로 비활성화하면 interceptor 통과 → 수동 우회 가능.

### D5. Polling + 동적 retryAfter

| 선택지 | 판단 |
|--------|------|
| **A) Polling + 동적 주기** | **채택** |
| B) SSE 브로드캐스트 | max-connections 경합, 구현 복잡 |
| C) 하이브리드 (SSE + Polling) | 두 모드 모두 구현/테스트 필요 |
| D) SSE 별도 서버 | 서비스 추가, 과제 범위 초과 |

**근거**: Stateless, 로드밸런서 친화적, 브라우저 호환성 100%. 동적 retryAfter(순번별 2/5/10초)로 Polling 부하 완화. SSE는 확장 포인트로만 설계.

| 순번 구간 | retryAfter |
|-----------|-----------|
| 1 ~ 100 | 2초 |
| 101 ~ 500 | 5초 |
| 501+ | 10초 |

### D6. 로그인 필수 (`@MemberAuthenticated`)

| 선택지 | 판단 |
|--------|------|
| **A) 로그인 필수** | **채택** |
| B) 비로그인 진입 허용 | 중복 방지 어려움, 봇 어뷰징 취약 |

**근거**: 주문이 목적이므로 어차피 인증 필요. memberId 기반 중복 방지가 자연스러움. ZADD NX로 동일 memberId 재진입 방지.

### D7. 무제한 + 설정값 외부화

| 선택지 | 판단 |
|--------|------|
| **A) 무제한 (설정 외부화)** | **채택** |
| B) 상한선 설정 (예: 50,000명) | 과제 범위에서 불필요 |

**근거**: Redis Sorted Set 멤버당 ~100B, 100만명 = ~100MB로 메모리 부담 낮음. `application.yml`에 설정값으로 외부화하여 필요 시 즉시 상한 적용 가능.

### D8. k6 부하테스트

| 선택지 | 판단 |
|--------|------|
| **A) k6** | **채택** |
| B) Gatling | 러닝커브, 무거움 |
| C) JMeter | 레거시, 스크립트 관리 어려움 |

**근거**: JS 기반 스크립트 간결. Docker 실행 가능. Grafana(monitoring-compose에 이미 존재)로 시각화 연동.

---

## 5. 시스템 흐름

### 5.1 전체 흐름

```
유저 → POST /queue/enter → 번호표 수령 (ZADD NX)
유저 → GET /queue/position → 전광판 확인 (ZRANK + retryAfter)
          ↕ (retryAfter 간격으로 반복)
스케줄러 (3초) → ZPOPMIN 300명 → 토큰 발급 (SET NX EX 300)
유저 → GET /queue/position → token 필드에 UUID 포함
유저 → POST /orders (X-Loopers-QueueToken: uuid) → 주문 생성
       → @TransactionalEventListener(AFTER_COMMIT) → 토큰 삭제
```

### 5.2 인터셉터 체인

```
HTTP Request (POST /api/v1/orders)
  → MemberAuthenticationInterceptor (인증, AuthenticatedMember 저장)
  → QueueTokenInterceptor (@QueueTokenRequired 체크)
      ├ 대기열 비활성 → 통과
      ├ 토큰 유효 → 통과
      ├ 토큰 무효/미제출 → 403
      └ Redis 장애 → 503 (fail-closed)
  → OrderV1Controller.createOrder()
```

### 5.3 스케줄러 흐름

```
QueueEntryScheduler (@Scheduled, 3초)
  1. 대기열 비활성 → skip
  2. SET NX EX "queue:scheduler:lock" {uuid} 5 → 실패 시 skip
  3. ZPOPMIN "queue:waiting" 300 → memberId 목록
  4. 각 memberId: Lua 원자적 토큰 발급 (SET NX EX + SADD)
  5. finally: Lua 안전 해제 (소유자 확인 후 DEL)
```

---

## 6. API 명세

### 6.1 대기열 API (`/api/v1/queue`)

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| POST | `/enter` | `@MemberAuthenticated` | 대기열 진입 |
| GET | `/position` | `@MemberAuthenticated` | 순번/토큰 조회 |

**POST /enter Response**
```json
{
  "meta": { "result": "SUCCESS" },
  "data": {
    "position": 42,
    "totalWaiting": 1350,
    "estimatedWaitSeconds": 3,
    "retryAfter": 2,
    "token": null
  }
}
```

**GET /position Response (토큰 발급 시)**
```json
{
  "meta": { "result": "SUCCESS" },
  "data": {
    "position": 0,
    "totalWaiting": 1200,
    "estimatedWaitSeconds": 0,
    "retryAfter": 0,
    "token": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

### 6.2 어드민 API (`/api-admin/v1/queue`)

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| POST | `/toggle` | `@AdminAuthenticated` | 대기열 ON/OFF |
| GET | `/status` | `@AdminAuthenticated` | 대기열 상태 조회 |

### 6.3 주문 API 변경

| Method | Path | 추가 헤더 | 설명 |
|--------|------|-----------|------|
| POST | `/api/v1/orders` | `X-Loopers-QueueToken: {uuid}` | 대기열 활성 시 토큰 필수 |

---

## 7. Redis 키 설계

| 키 | 타입 | TTL | 용도 |
|----|------|-----|------|
| `queue:waiting` | Sorted Set | 없음 | 대기열 (score=timestamp, member=memberId) |
| `queue:entry-token:{memberId}` | String | 5분 | 입장 토큰 (value=UUID) |
| `queue:entry-token:members` | Set | 없음 | 활성 토큰 보유 멤버 추적 (activeCount용) |
| `queue:config:enabled` | String | 없음 | 토글 ("true"/"false") |
| `queue:scheduler:lock` | String | 5초 | 분산 락 (SET NX EX) |

---

## 8. 처리량 설계

### 배치 크기 산정

```
DB 커넥션 풀: 50개
주문 평균 처리 시간: ~200ms (추정)
스케줄러 주기: 3초
커넥션 활용률 80%: 50 × 0.8 = 40 커넥션
3초간 처리 가능: 40 × (3000ms / 200ms) = 600건
안전 마진 50%: 600 × 0.5 = 300건
→ batchSize = 300
```

### Polling 부하 추정

동시 1만명 대기 시 (동적 retryAfter 적용):
- 1~100번: 100명 × (1/2초) = 50 req/s
- 101~500번: 400명 × (1/5초) = 80 req/s
- 501+: 9,500명 × (1/10초) = 950 req/s
- **총 ~1,080 req/s** (고정 3초 Polling 대비 3,333 → 1,080으로 67% 감소)

---

## 9. 패키지 구조

```
apps/commerce-api/src/main/kotlin/com/loopers/
├── domain/queue/
│   ├── QueuePosition.kt          # 순번, 예상 대기시간, retryAfter 계산
│   └── QueueErrorType.kt         # 대기열 전용 에러 타입
├── application/queue/
│   ├── QueueStore.kt             # 포트: Sorted Set 연산
│   ├── QueueTokenStore.kt        # 포트: 토큰 연산
│   ├── QueueConfigStore.kt       # 포트: 토글 플래그
│   ├── QueueInfo.kt              # 응답 DTO (QueueInfo, QueueStatusInfo)
│   ├── QueueService.kt           # 서비스 로직
│   └── QueueEntryScheduler.kt    # 분산 락 스케줄러
├── infrastructure/queue/
│   ├── RedisQueueStoreImpl.kt    # QueueStore Redis 구현
│   ├── RedisQueueTokenStoreImpl.kt   # Lua 원자적 토큰 + Set 추적
│   └── RedisQueueConfigStoreImpl.kt  # 토글 Redis 구현
├── interfaces/
│   ├── api/queue/                # QueueV1Controller, ApiSpec, Dto
│   ├── api/admin/queue/          # AdminQueueV1Controller, ApiSpec, Dto
│   └── config/auth/
│       ├── QueueTokenRequired.kt     # 어노테이션 (FUNCTION 타겟)
│       └── QueueTokenInterceptor.kt  # 토큰 검증 인터셉터
└── application/handler/
    ├── event/queue/QueueTokenEventHandler.kt       # AFTER_COMMIT 토큰 삭제
    └── command/queue/ConsumeQueueTokenCommandHandler.kt  # 토큰 삭제 실행
```

---

## 10. 부하테스트 — 역산 기반 SLO 검증

### 역산: 하드웨어 제약 → TPS 유도

```
시스템 제약: DB Pool=50, batchSize=300/3s, Token TTL=300s
SLO: p99 ≤ 500ms, 에러율 ≤ 0.1%

[Queue Entry — Redis bound]
  Redis ZADD NX ~50K OPS → 검증 목표: 1,000 RPS
  VU 산출 (Little's Law): 1000 × 0.05s = 50 → 마진 2x = 100 VUs

[Queue Polling — Redis bound]
  10K 대기자 / retryAfter 3s = 3,333 poll/s → 검증 목표: 2,000 RPS
  VU 산출: 2000 × 0.05s = 100 → 마진 2x = 200 VUs

[Order Processing — DB bound]
  스케줄러 100명/s (병목) < DB 250 TPS (한계) → 검증 목표: 100 TPS
  VU 산출: 100 × 0.3s = 30 → 마진 2x = 60 VUs
```

### 시나리오 5종

| 시나리오 | 목적 | VU/RPS | VU 근거 |
|----------|------|--------|---------|
| Smoke (11개) | 기능 검증 | 1-5 VUs | 시나리오별 최소 VU |
| Capacity | SLO 검증 (역산 기반) | 1K/2K/100 RPS | Little's Law + 2x 마진 |
| Load | 혼합 워크로드 | Peak 982 VUs | batchSize 기반 비율 분배 |
| Stress | 한계 탐색 | Peak 9,503 VUs | Load × 10배 |
| Spike | Thundering Herd | 0→5K (3초) | max-connections 50% |

### 테스��� 결과 (로컬 환경, 2026-04-04)

**Smoke — 11개 시나리오 전체 PASS**

| # | 시나리오 | 결과 |
|---|---------|------|
| 1 | E2E 풀플로우 (진입→토큰→주문) | PASS |
| 2 | 멱등 진입 | PASS |
| 3 | 토큰 없이 주문 → 403 | PASS |
| 4 | 위조 토큰 → 403 | PASS |
| 5 | 주문 후 재진입 | PASS |
| 6 | 순번 감소 검증 | PASS |
| 7 | 동시 중복 진입 → 멱등 | PASS |
| 8 | retryAfter 동적 조정 | PASS |
| 9 | 어드민 ���태 조회 | PASS |
| 10 | 트래픽 중 토글 OFF | PASS |
| 11 | 비활성 대기�� → 400 | PASS |

**Capacity — SLO 검증 (constant-arrival-rate)**

| 항목 | 목표 RPS | p99 응답 | 성공률 | SLO (p99≤500ms) |
|------|---------|---------|--------|-----------------|
| Entry | 1,000 | 15ms | 100% | **PASS** |
| Polling | 2,000 | 12ms | 100% | **PASS** |
| Order | 100 | 95ms | 100% | **PASS** |
| Admin | 5 | <500ms | 100% | **PASS** |

**Load/Stress/Spike**

| 시나리오 | Peak VUs | p95 | 실패율 | 판정 |
|---------|----------|-----|--------|------|
| Load | 982 | 30ms | 0% | **PASS** |
| Stress | 9,503 | 11.2s | 20.9% | **FAIL** (예상) |
| Spike | 10,000 | 10.4s | 27.1% | **FAIL** (예상) |

**병목 순서** (Stress/Spike에서 관측): BCrypt CPU → DB Pool 50 ��화 → accept-count 200 초과

### 실행 방법

```bash
# 서버 기동 후
k6 run k6/smoke.js      # 기능 검증 (11 시나리오)
k6 run k6/capacity.js   # 역산 기반 SLO 검증
k6 run k6/load.js       # 혼합 워크로드
k6 run k6/stress.js     # 한계 탐색
k6 run k6/spike.js      # Thundering Herd
```

---

## 11. 설정값

| 설정 | 기본값 | 위치 | 설명 |
|------|--------|------|------|
| `queue.batch-size` | 300 | application.yml | 스케줄러 1회 배치 크기 |
| `queue.scheduler-interval-seconds` | 3 | application.yml | 스케줄러 실행 주기 |
| `queue.token-ttl-seconds` | 300 | application.yml | 입장 토큰 TTL (5분) |

---

## 12. 주문 이후 흐름

대기열은 **주문 API 앞단의 관문**이다.
주문 API 이후의 흐름(이벤트 발행, Kafka 파이프라인, Metrics 집계)은 기존 Event-Command-Handler 구조를 그대로 활용한다.

```
주문 생성 → OrderRequestedEvent 발행
  → OrderRequestedEventHandler: 재고 차감 + 쿠폰 사용 + 주문 생성
  → QueueTokenEventHandler: 토큰 삭제 (AFTER_COMMIT)
  → 이후 결제 흐름 (기존 R7 구조)
```

---

## 13. 향후 확장

| 항목 | 현재 | 확장 방향 |
|------|------|-----------|
| 실시간 알림 | Polling | SSE 브로드캐스트 (순번 임박 유저) |
| Graceful Degradation | 503 fail-closed | Resilience4j CB + RateLimiter 통합 |
| 대기열 상한 | 무제한 | 설정값 기반 상한 + 503 응답 |
| Thundering Herd | retryAfter 동적 조절 | 토큰 발급 시 jitter (랜덤 지연) |
| 모니터링 | k6 + Grafana | Micrometer 커스텀 메트릭 (대기열 크기, 토큰 수) |
