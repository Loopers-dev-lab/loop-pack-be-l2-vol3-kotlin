# Round 7 — 이벤트 기반 아키텍처 & Kafka 파이프라인

## 개요

현재 주문-결제 플로우의 핵심/부가 로직을 ApplicationEvent로 분리하고,
Outbox + Kafka 파이프라인으로 시스템 간 이벤트 전파를 구현한다.
선착순 쿠폰 발급을 Kafka 기반 비동기 처리로 교체한다.

## 영향 범위

### 신규 파일

**commerce-api:**
- `application/event/` — Event sealed interface (PaymentEvent, CatalogEvent) + EventListener
- `domain/outbox/` — Outbox 도메인 모델 3종 (CatalogOutbox, OrderOutbox, CouponOutbox) + Repository 인터페이스
- `domain/coupon/model/CouponIssueRequest.kt` — 비동기 발급 요청 모델
- `infrastructure/outbox/` — Outbox JPA Entity + Repository 구현 3종
- `infrastructure/coupon/CouponIssueRequestEntity.kt` + Repository 구현
- `interfaces/support/scheduler/OutboxRelayScheduler.kt` — Outbox → Kafka Relay

**commerce-streamer:**
- `domain/` — ProductMetrics, EventHandled 모델 + Repository 인터페이스
- `infrastructure/` — JPA Entity + Repository 구현
- `interfaces/consumer/` — CatalogEventConsumer, OrderEventConsumer, CouponIssueConsumer

### 수정 파일

- `HandlePaymentCallbackUseCase.kt` — eventPublisher 주입, PaymentEvent 발행
- `AddLikeUseCase.kt` — CatalogEvent.LikeAdded 발행 추가
- `RemoveLikeUseCase.kt` — CatalogEvent.LikeRemoved 발행 추가
- `GetProductUseCase.kt` — eventPublisher 주입, CatalogEvent.ProductViewed 발행
- `IssueCouponUseCase.kt` → 비동기 발급으로 교체 (또는 신규 UseCase로 분리)
- `CouponV1Controller.kt` — 신규 엔드포인트 추가

### 관련 기존 파일 (패턴 참고)

- `ProductCacheEvent.kt` + `ProductCacheEventListener.kt` — sealed interface + @TransactionalEventListener 패턴
- `DemoKafkaConsumer.kt` — Kafka Consumer + manual Ack 패턴
- `PaymentRecoveryScheduler.kt` — 스케줄러 패턴

## 설계 결정 (확정)

1. **쿠폰 차감 타이밍**: **(A) 현재 흐름 유지**. `PlaceOrderUseCase`에서 `issuedCoupon.use()` 그대로 유지 (핵심 트랜잭션). `PaymentCompletedEvent`는 메트릭스 집계(판매량) 등 후속 처리만 트리거. 이유: 결제 완료 후로 이동하면 race condition 위험.
2. **포인트 적립**: **제외**. 현재 포인트 도메인이 없으며 신규 구현 범위 아님.
3. **상품 조회 이벤트 + readOnly**: **(C) AFTER_COMMIT 리스너에서 `@Transactional(REQUIRES_NEW)`로 별도 쓰기 트랜잭션**을 열어 Outbox 기록. `GetProductUseCase`의 readOnly는 유지. 이유: Outbox 경유 원칙 준수 + 조회 성능 유지.
4. **commerce-streamer DB**: **같은 DB 공유**. product_metrics, event_handled 테이블은 공유 DB에 위치.
5. **Outbox Relay 위치**: **commerce-api 스케줄러** (`PaymentRecoveryScheduler` 패턴 참고).

---

## 구현 계획

### Step 1 — ApplicationEvent로 경계 나누기

#### A. Event 클래스 (Application Layer)

- [ ] [P-A] A-1: [RED] PaymentEvent.Completed가 orderId, userId, totalAmount를 포함한다 → [GREEN] PaymentEvent sealed interface (Completed, Failed)
- [ ] [P-B] A-2: [RED] CatalogEvent가 productId, userId를 포함한다 → [GREEN] CatalogEvent sealed interface (LikeAdded, LikeRemoved, ProductViewed)

#### B. EventListener

- [ ] [P-A] B-1: [RED] PaymentEvent.Completed 발행 시 메트릭스 집계(판매량) 핸들러가 호출된다 → [GREEN] PaymentEventListener (@TransactionalEventListener AFTER_COMMIT)
- [ ] [P-B] B-2: [RED] CatalogEvent 발행 시 메트릭스 핸들러가 호출된다 → [GREEN] CatalogMetricsEventListener (@TransactionalEventListener AFTER_COMMIT)

#### C. 기존 UseCase 수정 (이벤트 발행 추가)

- [ ] [P-A] C-1: [RED] 결제 성공 콜백 시 PaymentEvent.Completed가 발행된다 → [GREEN] HandlePaymentCallbackUseCase에 eventPublisher 주입 + 발행
- [ ] [P-B] C-2: [RED] 좋아요 추가 시 CatalogEvent.LikeAdded가 발행된다 → [GREEN] AddLikeUseCase에 이벤트 발행 추가
- [ ] [P-B] C-3: [RED] 좋아요 삭제 시 CatalogEvent.LikeRemoved가 발행된다 → [GREEN] RemoveLikeUseCase에 이벤트 발행 추가
- [ ] [P-C] C-4: [RED] 상품 상세 조회 시 CatalogEvent.ProductViewed가 발행된다 → [GREEN] GetProductUseCase에 eventPublisher 주입 + 발행

--- checkpoint: Step 1 lint + test ---

### Step 2 — Outbox + Kafka (commerce-api 측)

#### D. Domain Model — Outbox

- [ ] [P-A] D-1: [RED] CatalogOutbox 생성 시 필수 필드(eventType, productId, userId)가 검증된다 → [GREEN] CatalogOutbox 도메인 모델
- [ ] [P-B] D-2: [RED] OrderOutbox 생성 시 필수 필드(eventType, orderId, userId)가 검증된다 → [GREEN] OrderOutbox 도메인 모델
- [ ] [P-C] D-3: [RED] CouponOutbox 생성 시 필수 필드(eventType, couponId, userId)가 검증된다 → [GREEN] CouponOutbox 도메인 모델

#### E. Repository 인터페이스 + Fake

- [ ] [P-A] E-1: [RED] CatalogOutboxRepository에서 미발행 메시지를 조회하고 발행 완료로 마킹한다 → [GREEN] 인터페이스 + FakeCatalogOutboxRepository
- [ ] [P-B] E-2: [RED] OrderOutboxRepository에서 미발행 메시지를 조회하고 발행 완료로 마킹한다 → [GREEN] 인터페이스 + FakeOrderOutboxRepository
- [ ] [P-C] E-3: [RED] CouponOutboxRepository에서 미발행 메시지를 조회하고 발행 완료로 마킹한다 → [GREEN] 인터페이스 + FakeCouponOutboxRepository

#### F. Infrastructure — Outbox (Entity + Impl)

- [ ] [P-A] F-1: [RED] CatalogOutbox를 DB에 저장하고 미발행 목록을 조회한다 → [GREEN] CatalogOutboxEntity + CatalogOutboxRepositoryImpl
- [ ] [P-B] F-2: [RED] OrderOutbox를 DB에 저장하고 미발행 목록을 조회한다 → [GREEN] OrderOutboxEntity + OrderOutboxRepositoryImpl
- [ ] [P-C] F-3: [RED] CouponOutbox를 DB에 저장하고 미발행 목록을 조회한다 → [GREEN] CouponOutboxEntity + CouponOutboxRepositoryImpl

#### G. EventListener → Outbox 전환

- [ ] G-1: [RED] CatalogEvent 발행 시 CatalogOutbox에 기록된다 → [GREEN] CatalogMetricsEventListener가 Outbox 기록으로 전환
- [ ] G-2: [RED] PaymentEvent.Completed 발행 시 OrderOutbox에 기록된다 → [GREEN] PaymentEventListener가 Outbox 기록으로 전환

#### H. Outbox Relay + Kafka Producer

- [ ] H-1: [RED] Relay 실행 시 미발행 Outbox 메시지가 Kafka로 발행되고 published=true로 마킹된다 → [GREEN] OutboxRelayScheduler
- [ ] H-2: Kafka Producer 설정 (acks=all, idempotence=true, partitionKey=aggregateId)

--- checkpoint: Step 2 commerce-api lint + test ---

### Step 2 — Kafka Consumer (commerce-streamer 측)

#### I. Domain Model

- [ ] [P-A] I-1: [RED] ProductMetrics 생성 시 productId가 필수이다 → [GREEN] ProductMetrics 모델 (productId, viewCount, likeCount, salesCount)
- [ ] [P-B] I-2: [RED] EventHandled에 eventId 존재 여부를 확인한다 → [GREEN] EventHandled 모델 (eventId PK, handledAt)

#### J. Repository + Infrastructure

- [ ] [P-A] J-1: [RED] ProductMetrics를 productId로 조회하고 upsert 한다 → [GREEN] Repository 인터페이스 + Fake + Entity + Impl
- [ ] [P-B] J-2: [RED] EventHandled를 eventId로 존재 여부 확인 + 저장한다 → [GREEN] Repository 인터페이스 + Fake + Entity + Impl

#### K. Consumer

- [ ] K-1: [RED] catalog-events 메시지 소비 시 ProductMetrics 조회수/좋아요가 갱신된다 → [GREEN] CatalogEventConsumer (멱등 처리 포함)
- [ ] K-2: [RED] order-events 메시지 소비 시 ProductMetrics 판매량이 갱신된다 → [GREEN] OrderEventConsumer (멱등 처리 포함)
- [ ] K-3: [RED] 이미 처리된 eventId의 메시지는 skip된다 → [GREEN] EventHandled 기반 중복 검사

--- checkpoint: Step 2 전체 lint + test ---

### Step 3 — 선착순 쿠폰 Kafka 발급

#### L. Domain Model

- [ ] L-1: [RED] CouponIssueRequest 생성 시 requestId(UUID), status=PENDING이 자동 설정된다 → [GREEN] CouponIssueRequest 모델

#### M. Repository

- [ ] M-1: [RED] CouponIssueRequest를 requestId로 조회한다 → [GREEN] CouponIssueRequestRepository 인터페이스 + FakeCouponIssueRequestRepository
- [ ] M-2: [RED] CouponIssueRequest를 DB에 저장하고 조회한다 → [GREEN] CouponIssueRequestEntity + CouponIssueRequestRepositoryImpl

#### N. Application (UseCase)

- [ ] N-1: [RED] 쿠폰 발급 요청 시 CouponIssueRequest(PENDING) 저장 + CouponOutbox 기록하고 requestId를 반환한다 → [GREEN] RequestCouponIssueUseCase
- [ ] N-2: [RED] requestId로 발급 결과를 조회하면 상태(PENDING/SUCCESS/FAILED/SOLD_OUT/DUPLICATE)를 반환한다 → [GREEN] GetCouponIssueStatusUseCase

#### O. Interfaces (API)

- [ ] O-1: [RED] POST /coupons/issue 요청 시 202 + requestId가 반환된다 → [GREEN] CouponV1Controller + CouponV1ApiSpec + Dto
- [ ] O-2: [RED] GET /coupons/issue/{requestId} 요청 시 발급 상태가 반환된다 → [GREEN] CouponV1Controller + CouponV1ApiSpec + Dto

--- checkpoint: Step 3 commerce-api lint + test ---

#### P. commerce-streamer — 쿠폰 Consumer

- [ ] P-1: [RED] coupon-issue-requests 소비 시 수량 잔여분이 있으면 쿠폰이 발급되고 SUCCESS로 저장된다 → [GREEN] CouponIssueConsumer
- [ ] P-2: [RED] 잔여 수량 0일 때 소비하면 SOLD_OUT으로 저장된다 → [GREEN] 수량 제한 로직
- [ ] P-3: [RED] 동일 userId+couponId 중복 소비 시 DUPLICATE로 저장된다 → [GREEN] 중복 발급 방지 로직

#### Q. 동시성 검증

- [ ] Q-1: [RED] N건 동시 발급 요청 시 설정 수량을 초과하지 않는다 → [GREEN] 단일 파티션 순차 처리 검증 (통합 테스트)

--- checkpoint: Step 3 전체 lint + test ---

## 고려사항

- **기존 패턴 일관성**: ProductCacheEvent sealed interface 패턴을 따라 새 이벤트도 sealed interface로 정의
- **기존 테스트 호환**: UseCase에 eventPublisher 추가 시 기존 단위 테스트에서 TestApplicationEventPublisher 또는 mock 처리 필요
- **Kafka 설정**: modules/kafka의 KafkaConfig 활용, Producer 설정 추가
- **DDL**: 신규 테이블 6개 (Outbox 3 + product_metrics + event_handled + coupon_issue_request) + 인덱스 다수
- **@Query 금지**: upsert 구현 시 QueryDSL 또는 findById + save 패턴 사용
- **세션 분할**: Step별로 변경 파일 5개 이상이므로 최소 3세션 (Step1, Step2, Step3)으로 분할 필요
