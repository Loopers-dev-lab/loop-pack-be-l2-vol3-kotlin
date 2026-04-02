# Round 7: Event-Driven Architecture 구현 계획

## Context

현재 모든 부가 로직(likeCount 업데이트, 캐시 무효화, 로깅)이 핵심 트랜잭션 안에 동기적으로 묶여 있다.
이번 라운드는 **왜 이벤트가 필요한가**를 체감하는 것이 목표다.

- ApplicationEvent로 핵심/부가 로직 경계를 나누고
- Kafka로 시스템 간 이벤트 파이프라인을 구축하고
- 선착순 쿠폰 발급에 실전 적용한다

**결정된 사항:**
- Consumer 앱: 기존 `commerce-streamer` 활용
- 좋아요 집계: eventual consistency 허용 (이벤트 분리)
- 쿠폰 수량 제어: Redis INCR 게이트 + Kafka

---

## Step 1 — ApplicationEvent로 경계 나누기

### 1.1 핵심 vs 부가 로직 분류

**판단 기준: "이 부가 로직이 실패하면, 핵심 동작도 실패해야 하는가?"**

| 플로우 | 핵심 (같은 TX) | 부가 (이벤트 분리) |
|--------|--------------|-----------------|
| **AddLike** | Like save | likeCount UPDATE, 캐시 무효화 |
| **CancelLike** | Like delete | likeCount UPDATE, 캐시 무효화 |
| **CreateOrder** | Order+Items 저장, 쿠폰 사용 처리 | 유저 행동 로깅 |
| **Payment 콜백 (성공)** | Payment approve, 재고 차감 | 캐시 무효화, 로깅 |
| **Payment 콜백 (실패)** | Payment fail, 주문 취소 | 캐시 무효화, 로깅 |
| **상품 조회** | 조회 응답 | 유저 행동 로깅 (조회수) |

### 1.2 이벤트 클래스 설계

```
domain/event/
  LikeCreatedEvent(userId, productId, occurredAt)
  LikeCancelledEvent(userId, productId, occurredAt)
  OrderCreatedEvent(orderId, userId, productIds, totalAmount, occurredAt)
  PaymentApprovedEvent(paymentId, orderId, userId, amount, productIds, occurredAt)
  PaymentFailedEvent(paymentId, orderId, userId, reason, occurredAt)
  UserActionEvent(userId, actionType, targetType, targetId, metadata, occurredAt)
```

### 1.3 리스너 설계

| 이벤트 | 리스너 | phase | @Async | WHY |
|--------|--------|-------|--------|-----|
| LikeCreatedEvent | LikeEventListener | AFTER_COMMIT | O | 커밋 확정 후에만 카운트. 비동기로 응답 차단 방지 |
| LikeCreatedEvent | CacheEvictionEventListener | AFTER_COMMIT | O | 커밋 전 무효화하면 stale read 가능 |
| LikeCancelledEvent | LikeEventListener | AFTER_COMMIT | O | 동일 |
| LikeCancelledEvent | CacheEvictionEventListener | AFTER_COMMIT | O | 동일 |
| OrderCreatedEvent | UserActionEventListener | AFTER_COMMIT | O | 주문 확정 후 로깅 |
| PaymentApprovedEvent | CacheEvictionEventListener | AFTER_COMMIT | O | 재고 차감 커밋 후 캐시 무효화 |
| PaymentApprovedEvent | UserActionEventListener | AFTER_COMMIT | O | 결제 성공 로깅 |
| PaymentFailedEvent | UserActionEventListener | AFTER_COMMIT | O | 결제 실패 로깅 |

**WHY 전부 AFTER_COMMIT:** 리스너 실패 시 핵심 TX 롤백 방지
**WHY 전부 @Async:** HTTP 응답 지연 방지

### 1.4 커밋 계획

```
1-1. ✅ feat: AsyncConfig + 도메인 이벤트 클래스 정의
1-2. ✅ refactor: 좋아요 플로우를 이벤트 기반으로 분리 (likeCount + 캐시 무효화)
1-3. ✅ refactor: 결제 콜백 플로우에서 캐시 무효화를 이벤트로 분리
1-4. ✅ feat: 유저 행동 로깅 이벤트 추가 (조회, 좋아요, 주문, 결제)
1-5. ✅ test: 이벤트 분리 통합 테스트 (실패 격리, eventual consistency)
```

### 1.5 테스트

| 레벨 | 테스트 | 검증 |
|------|--------|------|
| Unit | 이벤트 data class 생성 | 필드 정확성 |
| Integration | AddLikeUseCase + 이벤트 리스너 | Like 저장 → likeCount 비동기 반영 |
| Integration | **리스너 실패 격리 테스트** | likeCount 리스너가 예외 던져도 좋아요는 DB에 존재 |
| E2E | POST /likes → GET /products/{id} | likeCount가 eventually 반영됨 |

---

## Step 2 — Kafka 이벤트 파이프라인 + Transactional Outbox

### 2.1 어떤 이벤트가 Kafka로 가는가?

| 이벤트 | ApplicationEvent 유지 | Kafka 발행 | WHY |
|--------|:---:|:---:|-----|
| LikeCreated/Cancelled | O (likeCount는 같은 DB) | O (metrics 집계) | likeCount는 같은 서비스, metrics는 commerce-streamer |
| OrderCreated | O (로깅) | O (판매량 metrics) | 주문 집계는 cross-service |
| PaymentApproved/Failed | O (캐시 무효화) | O (매출 metrics) | 캐시는 같은 서비스, 매출 집계는 cross-service |
| UserActionEvent | - | O (전량) | 유저 행동 분석은 본질적으로 cross-service |

**핵심: 이중 발행 구조**
- UseCase TX 안에서: outbox 테이블에 기록 (같은 TX, 원자적)
- UseCase TX 커밋 후: ApplicationEvent 리스너가 in-process 부가 작업 수행
- 별도 릴레이: outbox → Kafka 발행

### 2.2 Transactional Outbox 패턴

**WHY Outbox인가:**
- TX 안에서 `kafkaTemplate.send()` → DB 롤백되어도 Kafka 메시지는 이미 발행됨
- TX 밖(AFTER_COMMIT)에서 produce → produce 실패 시 이벤트 유실
- Outbox: DB write + 이벤트 기록이 같은 TX → 원자적. 릴레이가 재시도 → at-least-once 보장

**WHY 스케줄러 릴레이 (CDC 아닌 이유):**
- CDC(Debezium)는 ms 단위 지연이지만 인프라 복잡도 높음
- 스케줄러 폴링은 1초 간격으로 충분. 프로젝트 스코프에 적합

### 2.3 테이블 스키마

**outbox_events**
```sql
CREATE TABLE outbox_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    partition_key VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    published_at DATETIME(6) NULL,
    INDEX idx_outbox_unpublished (published, created_at)
);
```

**product_metrics** (commerce-streamer)
```sql
CREATE TABLE product_metrics (
    product_id BIGINT PRIMARY KEY,
    view_count BIGINT NOT NULL DEFAULT 0,
    like_count BIGINT NOT NULL DEFAULT 0,
    order_count BIGINT NOT NULL DEFAULT 0,
    total_revenue BIGINT NOT NULL DEFAULT 0,
    last_event_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);
```

**event_handled** (commerce-streamer, 멱등성)
```sql
CREATE TABLE event_handled (
    event_id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    handled_at DATETIME(6) NOT NULL
);
```

### 2.4 토픽 설계

| 토픽 | Partition Key | WHY |
|------|:---:|----------|
| `catalog-events` | productId | 같은 상품의 like/unlike 순서 보장 |
| `order-events` | orderId | 같은 주문의 이벤트 순서 보장 |
| `coupon-issue-requests` | couponId | 같은 쿠폰의 발급 요청 FIFO 보장 (Step 3) |

### 2.5 커밋 계획

```
2-1. ✅ feat: Outbox 엔티티 + Repository + Writer 구현
2-2. ✅ feat: Outbox 릴레이 스케줄러 + Producer 설정 강화
2-3. (2-2에 병합)
2-4. ✅ refactor: UseCase에 outbox 기록 통합 (Like, Order, Payment)
2-5. ✅ feat: product_metrics + event_handled 엔티티 (commerce-streamer)
2-6. ✅ feat: CatalogEventConsumer + OrderEventConsumer + MetricsEventProcessor
2-7. (2-6에 병합)
2-8. ✅ test: Outbox 릴레이 통합 테스트 + Consumer 멱등성 테스트
```

### 2.6 테스트

| 레벨 | 테스트 | 검증 |
|------|--------|------|
| Unit | OutboxEvent.create() | eventId 생성, payload 직렬화 |
| Integration | OutboxEventRelay | 미발행 이벤트 조회 → Kafka produce → published 마킹 |
| Integration | Consumer 멱등성 | 같은 event_id 2번 처리 → 두 번째는 skip |
| Integration | CatalogEventConsumer | LIKE_CREATED → product_metrics.like_count +1 |
| E2E | 좋아요 → Kafka → product_metrics | 전체 파이프라인 end-to-end |

---

## Step 3 — Kafka 기반 선착순 쿠폰 발급

### 3.1 아키텍처

```
사용자 → POST /api/v1/coupons/{id}/issue-async
  → 쿠폰 유효성 검증
  → Redis INCR coupon:{couponId}:counter
    → 초과 시: DECR + 즉시 SOLD_OUT 응답
    → 통과 시: CouponIssueRequest(PENDING) 저장 + outbox 기록 (같은 TX)
              → 202 Accepted + requestId 반환

OutboxRelay → Kafka topic: coupon-issue-requests (key=couponId)

commerce-streamer Consumer:
  → event_handled 멱등성 체크
  → DB unique constraint (user_id, coupon_id) 로 중복 방어
  → DB COUNT < maxQuantity 확인 (ground truth)
  → UserCoupon 생성
  → CouponIssueRequest 상태 업데이트 (ISSUED/REJECTED)

사용자 → GET /api/v1/coupons/issue-requests/{requestId}
  → 상태 조회: PENDING / ISSUED / REJECTED / SOLD_OUT
```

### 3.2 WHY Redis INCR 게이트

- Redis 없이: 10,000건 전부 Kafka → Consumer가 9,900건 거절 (낭비)
- Redis INCR: 100건만 Kafka 통과, 9,900건은 API에서 즉시 sold out 응답

**Redis 카운터 ↔ 실제 발급 수 불일치 처리:**
| 시나리오 | 해결 |
|---------|------|
| INCR 성공 → Kafka 실패 | API에서 catch → DECR |
| INCR 성공 → Consumer 거절 (중복) | Consumer에서 DECR |
| Redis 재시작 (카운터 초기화) | Consumer가 DB COUNT로 최종 검증 (ground truth) |

### 3.3 커밋 계획

```
3-1. ✅ feat: Coupon에 maxQuantity 필드 추가
3-2. ✅ feat: CouponIssueRequest 엔티티 + Repository + Redis 카운터
3-3. (3-2에 병합)
3-4. ✅ feat: RequestCouponIssueAsyncUseCase (Redis 게이트 → outbox)
3-5. ✅ feat: GetCouponIssueResultUseCase + 폴링 API
3-6. ✅ feat: CouponIssueConsumer + CouponIssueProcessor (commerce-streamer)
3-7. ✅ test: 동시성 테스트 — 100 요청 / 10장 쿠폰 → 정확히 10장 발급
3-8. ✅ test: Redis 카운터 보정 테스트 + INCR 후 실패 시 DECR 보정 로직 추가
```

### 3.4 테스트

| 레벨 | 테스트 | 검증 |
|------|--------|------|
| Unit | Redis 카운터 INCR/DECR | 경계값 (max 도달, 초과) |
| Integration | 비동기 발급 플로우 | 요청 → Kafka → Consumer → UserCoupon 생성 |
| Integration | 중복 발급 방지 | 같은 유저 2번 요청 → 1번만 발급 |
| Concurrency | 1000 스레드 × 10장 쿠폰 | 정확히 10장만 발급, 초과 없음 |
| E2E | POST issue-async → GET result | PENDING → ISSUED 상태 전이 |

---

## 핵심 트레이드오프 요약

| 결정 | 선택 | 대안 | WHY |
|------|------|------|-----|
| likeCount 업데이트 | 이벤트 분리 (eventual) | 같은 TX 유지 | 카운터 정확도 < 좋아요 성공 보장 |
| Outbox 릴레이 | @Scheduled 폴링 (1초) | CDC (Debezium) | 인프라 단순. 1초 지연 수용 가능 |
| 쿠폰 수량 게이트 | Redis INCR + Kafka | 순수 Kafka | 대량 요청 99% 사전 필터링 |
| stock 차감 | 핵심 TX 유지 | 이벤트 분리 | 재무 정합성. 팬텀 재고 불가 |
| Outbox 기록 방식 | UseCase에서 직접 호출 | @EventListener(same TX) | 명시적, 테스트 용이 |
| metrics 저장 | 별도 product_metrics 테이블 | Product.likeCount를 Kafka로 갱신 | 분석 스키마와 핵심 도메인 분리 |
