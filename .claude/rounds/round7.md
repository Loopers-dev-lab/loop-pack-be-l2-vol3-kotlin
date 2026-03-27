# Round 7: Event-Driven Architecture

## 목표
핵심/부가 로직 경계를 이벤트로 분리하고, Kafka 파이프라인 구축 + 선착순 쿠폰 발급 적용

---

## Step 1 — ApplicationEvent로 경계 나누기

### 커밋 1-1: AsyncConfig + 도메인 이벤트 클래스 정의

**의사결정:**

1. **이벤트 클래스를 domain/event/ 에 배치한 이유**
   - 이벤트는 도메인에서 "무엇이 일어났는가"를 표현하는 도메인 개념
   - Spring 의존성 없는 순수 data class → 도메인 레이어 원칙 준수
   - UseCase에서 발행하고, Application 레이어의 리스너가 소비

2. **AsyncConfig 설계 결정**
   - core=2, max=10, queue=100: 부가 로직(카운터 업데이트, 캐시 무효화) 처리 규모에 적합
   - `setWaitForTasksToCompleteOnShutdown(true)`: 앱 종료 시 진행 중인 이벤트 처리 완료 보장
   - `AsyncUncaughtExceptionHandler`: 비동기 이벤트 실패를 로그로 남겨 모니터링 가능

3. **이벤트에 occurredAt 기본값을 넣은 이유**
   - 이벤트 생성 시점 = 도메인 행위 발생 시점. 발행 시점과 분리하기 위해 생성자에서 기록
   - 기본값 `LocalDateTime.now()`로 보일러플레이트 감소

4. **PaymentApprovedEvent에 productIds를 포함한 이유**
   - 캐시 무효화 리스너가 어떤 상품 캐시를 날려야 하는지 알아야 함
   - 리스너에서 다시 DB 조회하면 이벤트 분리 의미 퇴색 → 이벤트에 필요한 정보 포함

**트레이드오프:**

| 결정 | 선택 | 대안 | WHY |
|------|------|------|-----|
| ThreadPool 사이즈 | core=2/max=10 | core=5/max=20 | 부가 로직은 경량 작업. 과도한 스레드는 리소스 낭비 |
| 이벤트 위치 | domain/event/ | application/event/ | 이벤트는 "무슨 일이 일어났나"를 표현 → 도메인 개념 |
| 이벤트 상속 | 없음 (각각 독립 data class) | sealed interface DomainEvent | YAGNI. 공통 처리 필요해지면 그때 추가 |

### 커밋 1-2: 좋아요 플로우를 이벤트 기반으로 분리

- AddLikeUseCase/CancelLikeUseCase에서 likeCount/캐시 직접 호출 제거 → ApplicationEvent 발행
- LikeEventListener: `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` + `TransactionTemplate`
- 테스트: Awaitility로 eventual consistency 검증

**발견한 이슈:**
- Spring 6.1+ `RestrictedTransactionalEventListenerFactory`: `@Transactional` + `@TransactionalEventListener` 같은 메서드 금지 → `TransactionTemplate`으로 해결
- `ThreadPoolTaskExecutor.initialize()` 수동 호출: Spring Bean 라이프사이클 충돌 → 제거

### 커밋 1-3: 결제 콜백 플로우에서 캐시 무효화를 이벤트로 분리

**의사결정:**

1. **HandlePaymentCallbackUseCase + PaymentTransactionManager 양쪽 모두 변경한 이유**
   - 결제 콜백(PG에서 호출)과 복구 스케줄러(applyPgResult) 두 경로 모두 재고 차감 + 캐시 무효화가 있었음
   - 두 경로 모두 동일한 패턴으로 이벤트 발행으로 전환

2. **deductStock()이 productIds를 반환하도록 변경한 이유**
   - 이벤트에 productIds가 필요 (캐시 무효화 대상)
   - 재고 차감은 핵심 TX 안에서 수행 → 그 결과인 productIds를 이벤트에 전달

3. **PaymentFailedEvent 리스너가 로깅만 하는 이유**
   - 결제 실패 시 캐시 무효화 불필요 (재고 변경 없음)
   - 주문 취소 + 쿠폰 반환은 핵심 TX로 유지 (정합성 필수)
   - 향후 Kafka 발행 시 metrics 집계에 활용

4. **Product CRUD의 AfterCommit은 남겨둔 이유**
   - 상품 등록/수정/삭제는 관리자 작업으로 빈도 낮음
   - 이벤트 분리의 실익이 작음 → 현재 스코프 밖

**트레이드오프:**

| 결정 | 선택 | 대안 | WHY |
|------|------|------|-----|
| 재고 차감 | 핵심 TX 유지 | 이벤트 분리 | 재무 정합성. 팬텀 재고 불가 |
| 쿠폰 반환 | 핵심 TX 유지 | 이벤트 분리 | 이중 사용 방지. 정합성 필수 |
| 캐시 무효화 | 이벤트 분리 | AfterCommit 유지 | 캐시 실패로 결제 롤백은 과도한 결합 |
| Product CRUD AfterCommit | 유지 | 이벤트 전환 | 관리자 작업, 빈도 낮음, 현재 스코프 밖 |

### 커밋 1-4: 유저 행동 로깅 이벤트 추가

**의사결정:**

1. **기존 도메인 이벤트를 재활용하는 구조를 선택한 이유**
   - UserActionEvent data class를 별도로 발행하지 않고, 기존 LikeCreatedEvent/OrderCreatedEvent 등을 수신
   - 이벤트 발행을 이중으로 하면 UseCase가 복잡해지고 이벤트 순서 보장도 어려움
   - 하나의 도메인 이벤트에 여러 리스너가 반응하는 것이 이벤트 아키텍처의 자연스러운 패턴

2. **LoggerFactory.getLogger("UserActionLog") 전용 로거 사용 이유**
   - 클래스 이름 대신 논리적 이름으로 분류하면 로그 수집 시 필터링 용이
   - 향후 Kafka UserAction 토픽과 1:1 매핑 가능

3. **상품 조회(VIEW) 로깅을 이번 커밋에서 제외한 이유**
   - GetProductUseCase는 userId를 모름 (읽기 전용, 비로그인도 조회 가능)
   - 조회 로깅은 Controller 레이어에서 처리하거나 Step 2 Kafka에서 별도 구현이 적합
   - UseCase에 userId를 억지로 넣으면 레이어 책임이 흐려짐

4. **CreateOrderUseCase에 OrderCreatedEvent 발행 추가**
   - 주문 생성은 핵심 비즈니스 이벤트 → 로깅뿐 아니라 Step 2 Kafka 발행에도 활용

### 커밋 1-5: 이벤트 분리 통합 테스트 (실패 격리, eventual consistency)

**의사결정:**

1. **실패 격리 테스트 설계**
   - 핵심 검증: AddLikeUseCase의 @Transactional 커밋 → LikeEventListener 비동기 실행 구조에서, 좋아요 데이터가 이벤트 처리와 무관하게 DB에 존재하는지
   - 이벤트 분리의 핵심 가치 = "부가 로직 실패가 핵심 동작에 영향을 주지 않는 것"

2. **Eventual Consistency 테스트 설계**
   - 단일 유저 좋아요 → likeCount 비동기 반영 (Awaitility)
   - 다중 유저 좋아요 → 3개 이벤트 모두 비동기 반영 검증
   - 기존 AddLikeUseCaseTest에도 Awaitility가 있지만, 이벤트 리스너 전용 테스트로 명시적 검증

3. **테스트 위치를 application/like/event/ 에 배치한 이유**
   - LikeEventListener와 동일한 패키지 구조로 테스트 배치
   - 이벤트 리스너의 동작을 직접 검증하는 통합 테스트

**트레이드오프:**

| 결정 | 선택 | 대안 | WHY |
|------|------|------|-----|
| 실패 격리 검증 방식 | 정상 플로우 후 Like 존재 확인 | Mock으로 리스너 강제 실패 | 실제 코드의 try-catch가 격리를 담당. 정상 플로우에서도 Like가 이벤트 처리 전에 커밋됨을 검증하는 것이 핵심 |
| Awaitility timeout | 3~5초 | 1초 | CI 환경에서 비동기 스레드 스케줄링 지연 고려 |

---

## Step 2 — Kafka 이벤트 파이프라인 + Transactional Outbox

### 커밋 2-1: Outbox 엔티티 + Repository + Writer 구현

**의사결정:**

1. **Transactional Outbox 패턴을 채택한 이유**
   - TX 안에서 `kafkaTemplate.send()` → DB 롤백되어도 Kafka 메시지는 이미 발행됨 (회수 불가)
   - TX 밖(AFTER_COMMIT)에서 produce → produce 실패 시 이벤트 유실
   - Outbox: DB write + 이벤트 기록이 같은 TX → 원자적. 릴레이가 재시도 → at-least-once 보장

2. **OutboxEvent를 BaseEntity 상속한 이유**
   - id, createdAt, updatedAt 자동 관리
   - published/publishedAt는 outbox 고유 필드이므로 직접 선언

3. **eventId를 UUID로 생성하는 이유**
   - Consumer 측 멱등성 키로 사용 (event_handled 테이블의 PK)
   - DB auto-increment id는 서비스 간 공유에 부적합 (내부 구현 노출)

4. **OutboxEventType enum에 topic을 매핑한 이유**
   - 이벤트 타입 → 토픽 라우팅을 한 곳에서 관리
   - UseCase에서 토픽명을 하드코딩하지 않음
   - catalog-events: 좋아요 (partitionKey=productId) → 같은 상품 순서 보장
   - order-events: 주문/결제 (partitionKey=orderId) → 같은 주문 순서 보장

5. **OutboxEventWriter를 Application 레이어에 배치한 이유**
   - UseCase에서 직접 호출 (같은 TX 내)
   - ObjectMapper로 payload 직렬화 → 인프라 의존성이지만, Writer가 중개 역할

6. **payload를 JSON 문자열로 저장하는 이유**
   - Kafka로 발행 시 그대로 전송 가능 (재직렬화 불필요)
   - DB에서 디버깅 시 사람이 읽을 수 있음
   - 스키마 변경에 유연 (컬럼 추가 없이 payload 구조 변경 가능)

**트레이드오프:**

| 결정 | 선택 | 대안 | WHY |
|------|------|------|-----|
| 이벤트 발행 방식 | Transactional Outbox | 직접 Kafka produce | DB-Kafka 원자성 보장. at-least-once 달성 |
| Outbox 릴레이 방식 | @Scheduled 폴링 (다음 커밋) | CDC (Debezium) | 인프라 단순. 1초 지연은 수용 가능 |
| payload 저장 형식 | JSON 문자열 | 바이너리 (Avro/Protobuf) | 가독성, 디버깅 용이. 성능 차이 미미 |
| partitionKey | 비즈니스 키 (productId/orderId) | 랜덤 | 같은 엔티티 이벤트 순서 보장 |
| Outbox 기록 방식 | UseCase에서 직접 호출 | @EventListener(same TX) | 명시적, 테스트 용이, 이벤트 라이프사이클 복잡성 회피 |

### 커밋 2-2: Outbox 릴레이 스케줄러 + Producer 설정 강화

**의사결정:**

1. **릴레이 스케줄러 설계**
   - `@Scheduled(fixedDelay = 1_000)`: 1초 간격 폴링. CDC 대비 지연 있지만 인프라 단순
   - `BATCH_SIZE = 100`: 한 번에 100건씩 처리. 과도한 DB 조회 방지
   - `kafkaTemplate.send().get()`: 동기 전송으로 발행 확인 후 published 마킹
   - 실패 시 다음 스케줄에서 재시도 (at-least-once)

2. **kafkaTemplate.send().get() 동기 호출을 선택한 이유**
   - Outbox 릴레이는 백그라운드 스케줄러 → 응답 지연 무관
   - 비동기 send() 후 콜백에서 markPublished → 콜백 실패 시 이중 발행 위험
   - 동기 get()으로 Kafka 브로커 ACK 확인 → markPublished 순서 보장

3. **markPublished()를 TransactionTemplate으로 감싼 이유**
   - @Scheduled는 트랜잭션이 없음
   - dirty checking이 동작하려면 트랜잭션 필요
   - 각 이벤트별 독립 트랜잭션으로 하나의 실패가 전체를 롤백하지 않음

4. **Producer 설정 강화 (acks=all, idempotence=true)**
   - `acks=all`: 모든 ISR replica 확인 → 메시지 유실 방지
   - `enable.idempotence=true`: 네트워크 재시도 시 중복 방지 (PID + sequence number)
   - `max.in.flight.requests.per.connection=5`: 멱등성 보장 상한 (Kafka 공식 권장)

5. **commerce-api에 kafka 모듈 의존성 추가**
   - Outbox 릴레이가 KafkaTemplate을 사용하므로 kafka 모듈 필요
   - application.yml에 kafka.yml import 추가

**트레이드오프:**

| 결정 | 선택 | 대안 | WHY |
|------|------|------|-----|
| 릴레이 주기 | 1초 고정 지연 | 이벤트 감지 시 즉시 | 구현 단순. 1초 지연은 metrics 집계에 충분 |
| Kafka send 방식 | 동기 (.get()) | 비동기 (콜백) | 릴레이는 백그라운드. 순서 보장 + 발행 확인 우선 |
| acks | all | 1 (리더만) | 메시지 유실 방지가 outbox 패턴의 핵심 가치 |
| 멱등성 | enable.idempotence=true | false | 네트워크 재시도 시 중복 메시지 방지 |

### 커밋 2-4: UseCase에 outbox 기록 통합 (Like, Order, Payment)

**의사결정:**

1. **이중 발행 구조를 채택한 이유**
   - ApplicationEvent: 같은 서비스 내 부가 로직 (likeCount, 캐시 무효화)
   - Outbox: 서비스 간 이벤트 전파 (metrics 집계 등)
   - 같은 이벤트 객체를 두 곳에 전달 → 페이로드 일관성 유지

2. **outboxEventWriter.write()를 UseCase에서 명시적으로 호출한 이유**
   - `@EventListener`로 같은 TX에서 자동 기록하는 대안 고려
   - 명시적 호출이 코드 가독성 + 테스트 용이성에서 우월
   - 어떤 이벤트가 Kafka로 가는지 UseCase 코드에서 바로 확인 가능

3. **partitionKey 선택 기준**
   - Like 이벤트 → productId: 같은 상품의 like/unlike 순서 보장 (카운터 음수 방지)
   - Order/Payment 이벤트 → orderId: 같은 주문의 생성→결제 순서 보장

4. **PaymentTransactionManager에도 outbox 기록을 추가한 이유**
   - HandlePaymentCallbackUseCase(PG 콜백)와 applyPgResult(복구 스케줄러) 두 경로 존재
   - 두 경로 모두 Kafka로 이벤트 전파 필요 → 둘 다 outbox 기록

**트레이드오프:**

| 결정 | 선택 | 대안 | WHY |
|------|------|------|-----|
| outbox 기록 위치 | UseCase에서 직접 호출 | @EventListener 자동 기록 | 명시적. "이 이벤트가 Kafka로 간다"를 코드에서 확인 가능 |
| 이벤트 객체 공유 | ApplicationEvent와 outbox가 같은 data class 사용 | 별도 DTO | YAGNI. 현재는 동일 페이로드. 분기 필요 시 분리 |
| Like partitionKey | productId | userId | 같은 상품의 like/unlike FIFO 보장이 카운터 정합성에 중요 |

### 커밋 2-5: product_metrics + event_handled 엔티티 (commerce-streamer)

**의사결정:**

1. **product_metrics를 별도 테이블로 분리한 이유**
   - Product 엔티티의 likeCount와 별도로 분석 전용 집계 테이블
   - 핵심 도메인(Product)과 분석 스키마의 독립적 진화 가능
   - view_count, order_count, total_revenue 등 분석 필드 추가에 Product 변경 불필요

2. **ProductMetrics의 PK를 productId로 설정한 이유**
   - auto-increment id 불필요 — 상품당 1row, productId가 자연키
   - findOrCreate 패턴으로 upsert 처리

3. **event_handled로 Consumer 멱등성을 보장하는 이유**
   - Outbox 릴레이는 at-least-once → 같은 이벤트가 2번 발행될 수 있음
   - Consumer가 eventId로 중복 체크 → 두 번째 처리는 skip
   - eventId = UUID (outbox의 event_id와 동일)

4. **@Version(낙관적 락)을 product_metrics에 적용한 이유**
   - 동시에 같은 상품에 like + order 이벤트가 도착할 수 있음
   - 낙관적 락으로 충돌 감지 → 재시도 가능

5. **EventHandled에 BaseEntity를 상속하지 않은 이유**
   - PK가 eventId(String) → BaseEntity의 id(Long) auto-increment와 충돌
   - handledAt만 필요하므로 독립적으로 정의

**트레이드오프:**

| 결정 | 선택 | 대안 | WHY |
|------|------|------|-----|
| metrics 저장 | 별도 product_metrics 테이블 | Product.likeCount를 Kafka로 갱신 | 분석 스키마와 핵심 도메인 분리. 독립 진화 |
| 멱등성 보장 | event_handled 테이블 | Redis Set | DB 기반이 더 안정적. 영구 저장 |
| 동시성 제어 | @Version (낙관적 락) | 비관적 락 (SELECT FOR UPDATE) | 충돌 빈도 낮음. 재시도로 해결 |

### 커밋 2-6/2-7: CatalogEventConsumer + OrderEventConsumer + MetricsEventProcessor

**의사결정:**

1. **Kafka 헤더에 eventId/eventType을 넣은 이유**
   - Consumer가 멱등성 체크를 위해 eventId 필요
   - payload 파싱 전에 헤더로 빠르게 확인 가능
   - Outbox 릴레이에서 ProducerRecord 헤더에 추가

2. **MetricsEventProcessor를 별도 서비스로 분리한 이유**
   - Consumer(Interfaces)는 메시지 수신 + 파싱만 담당
   - 비즈니스 로직(멱등성 체크, metrics 업데이트)은 Application 레이어
   - 레이어 책임 분리 유지 + 테스트 용이

3. **배치 리스너를 사용하되 건별 처리하는 이유**
   - `@KafkaListener(containerFactory = BATCH_LISTENER)`: 배치로 수신하여 네트워크 효율
   - 각 메시지는 forEach로 개별 처리: 하나의 실패가 전체 배치를 롤백하지 않음
   - 배치 전체 처리 후 `acknowledgment.acknowledge()`: 한 번에 ACK

4. **PaymentFailedEvent를 로깅만 하는 이유**
   - 결제 실패 시 metrics 변경 없음 (재고/매출 영향 없음)
   - 향후 실패율 모니터링/알림에 활용 가능

5. **Consumer groupId를 토픽별로 분리한 이유**
   - `commerce-streamer-catalog`: catalog-events 전용
   - `commerce-streamer-order`: order-events 전용
   - 토픽별 독립적인 오프셋 관리 + 장애 격리

**트레이드오프:**

| 결정 | 선택 | 대안 | WHY |
|------|------|------|-----|
| 이벤트 메타데이터 전달 | Kafka 헤더 | payload에 포함 | payload는 도메인 이벤트 그대로 유지. 메타데이터는 인프라 관심사 |
| payload 파싱 | ObjectMapper.readTree (JsonNode) | 타입별 DTO 역직렬화 | 유연성. 새 필드 추가 시 DTO 변경 불필요 |
| 에러 처리 | 건별 try-catch + 로깅 | 배치 전체 재시도 | 하나의 poison pill이 전체 배치를 차단하지 않음 |
| ACK 전략 | 배치 끝에 한 번 | 건별 ACK | 배치 ACK이 효율적. 실패 메시지는 멱등성으로 재처리 |

### 커밋 2-8: Outbox 릴레이 통합 테스트 + Consumer 멱등성 테스트

**의사결정:**

1. **OutboxEventIntegrationTest (commerce-api) 설계**
   - 좋아요 등록/취소 시 outbox_events 테이블에 올바른 이벤트가 기록되는지 검증
   - eventType, topic, partitionKey, published 상태까지 확인
   - UseCase → outboxEventWriter → outboxEventRepository 전체 흐름 통합 테스트

2. **MetricsEventProcessorTest (commerce-streamer) 설계**
   - Consumer가 아닌 MetricsEventProcessor를 직접 호출하여 비즈니스 로직 검증
   - 멱등성 테스트: 같은 eventId로 2번 호출 → likeCount 1번만 증가
   - Metrics 업데이트 테스트: likeCount 증가/감소, totalRevenue 반영
   - Kafka Consumer 테스트가 아닌 Application 레이어 테스트로 격리

3. **docker-java.properties 추가 (commerce-streamer, commerce-batch)**
   - Docker Desktop 4.57+ 에서 API 버전 자동 협상 시 BadRequest(400) 응답 발생
   - `api.version=1.44` 명시로 해결 (commerce-api에는 이미 존재)
   - 모든 앱 모듈에 동일한 설정 적용하여 일관성 확보

4. **테스트 범위를 Processor 단위로 한정한 이유**
   - Kafka Consumer 통합 테스트는 Testcontainers Kafka 기동 시간 부담 큼
   - MetricsEventProcessor의 비즈니스 로직 (멱등성, metrics 업데이트)이 핵심
   - Consumer는 메시지 파싱 + Processor 위임만 담당 → 별도 검증 필요성 낮음

**트레이드오프:**

| 결정 | 선택 | 대안 | WHY |
|------|------|------|-----|
| 테스트 범위 | Processor 단위 테스트 | Consumer → Kafka E2E | Processor 로직이 핵심. E2E는 기동 비용 대비 추가 검증값 낮음 |
| Docker API 버전 | 1.44 고정 | 자동 협상 | Docker Desktop 4.57+ 호환. 명시적 버전이 안정적 |
| Outbox 기록 검증 | 필드별 assertAll | 단순 존재 확인 | eventType, topic, partitionKey까지 검증해야 라우팅 정확성 보장 |

---

## Step 3 — Kafka 기반 선착순 쿠폰 발급

### 커밋 3-1: Coupon에 maxQuantity 필드 추가

**의사결정:**

1. **maxQuantity를 nullable(Int?)로 설계한 이유**
   - 기존 쿠폰은 수량 제한 없이 동작 → null = 무제한
   - 새로운 선착순 쿠폰만 maxQuantity 설정
   - 기존 코드 변경 최소화 (null이면 기존 로직 그대로)

2. **isLimited() 메서드를 추가한 이유**
   - `maxQuantity != null` 조건을 도메인 메서드로 캡슐화
   - UseCase에서 "이 쿠폰이 수량 제한 쿠폰인가?" 판단 시 사용

3. **validateMaxQuantity()를 Entity에서 검증하는 이유**
   - "수량은 1 이상이어야 한다" → 현실 세계에서도 참인 비즈니스 규칙
   - 도메인 불변식 → Entity의 create()에서 검증

### 커밋 3-2: CouponIssueRequest 엔티티 + Repository + Redis 카운터

**의사결정:**

1. **CouponIssueRequest를 별도 엔티티로 분리한 이유**
   - 비동기 발급의 "요청 상태 추적" 책임
   - UserCoupon(발급 결과)과는 다른 생명주기 — 요청은 PENDING→ISSUED/REJECTED 전이
   - 폴링 API에서 requestId로 조회

2. **requestId를 UUID로 생성하는 이유**
   - 클라이언트에게 반환하는 폴링 키 → DB id(Long) 노출 방지
   - 외부 API 응답에 적합한 식별자

3. **CouponCounterStore 인터페이스를 Domain에 배치한 이유**
   - DIP: UseCase가 Redis 구현에 직접 의존하지 않음
   - 테스트 시 Fake 구현 가능
   - increment()/decrement() — 도메인 관점의 카운터 연산만 노출

4. **Redis 키 패턴을 `coupon:{couponId}:counter`로 설계한 이유**
   - 네임스페이스 분리 (다른 Redis 키와 충돌 방지)
   - couponId로 개별 쿠폰 카운터 관리

### 커밋 3-4: RequestCouponIssueAsyncUseCase (Redis 게이트 → outbox)

**의사결정:**

1. **Redis INCR 게이트를 채택한 이유**
   - 선착순 100장에 1만 명 동시 요청 시: Redis 없이 → 10,000건 전부 Kafka 통과 → Consumer 99% 거절
   - Redis INCR: O(1) 원자적 연산 → 100건만 Kafka 통과, 9,900건 즉시 SOLD_OUT 응답
   - API 레이어에서 대부분 필터링 → Kafka/Consumer 부하 99% 감소

2. **INCR → 초과 시 DECR + 예외의 패턴을 선택한 이유**
   - 원자적 INCR 후 초과 판단 → DECR로 카운터 보정
   - Lua 스크립트(INCR + 조건부 DECR)도 가능하지만 단일 INCR/DECR로 충분

3. **INCR 후 DB 실패 시 DECR 보정 (try-catch) 추가 이유**
   - Redis INCR 성공 → DB 저장 실패(TX 롤백) → 카운터만 올라가고 요청은 없는 상태
   - try-catch로 DB 실패 감지 → DECR 보정 → 카운터 정확성 유지
   - 보정 없으면 실제 발급 가능 수량 < Redis 카운터 → 일부 유저가 불필요한 SOLD_OUT

4. **existsByUserIdAndCouponId 선행 검증의 한계를 인지한 이유**
   - user_coupons 테이블 기반 검증 → 비동기 플로우에서는 Consumer가 아직 UserCoupon을 생성하지 않았을 수 있음
   - 동기 발급(IssueCouponUseCase) 이후의 재요청 방지 역할
   - 비동기 동시 요청 시 중복은 Consumer의 unique constraint가 최종 방어

5. **outbox에 기록하는 이벤트에 requestId를 포함한 이유**
   - Consumer가 처리 결과를 CouponIssueRequest 상태에 반영해야 함
   - requestId로 요청 상태 업데이트 (PENDING → ISSUED/REJECTED)

**트레이드오프:**

| 결정 | 선택 | 대안 | WHY |
|------|------|------|-----|
| 수량 게이트 | Redis INCR | 순수 Kafka (Consumer에서 전량 필터) | 대량 요청 99% 사전 필터링. 즉시 응답 |
| 초과 처리 | INCR + DECR | Lua 스크립트 (atomic check-and-increment) | 단순 INCR/DECR로 충분. Lua 복잡성 불필요 |
| 중복 방어 | API 선검증 + Consumer unique constraint | API만 | API 검증은 race condition 존재. Consumer가 ground truth |
| 실패 보정 | try-catch DECR | 보정 없음 (ground truth에 위임) | 카운터 정확성 유지. 불필요한 SOLD_OUT 방지 |

### 커밋 3-5: GetCouponIssueResultUseCase + 폴링 API

**의사결정:**

1. **202 Accepted + 폴링 패턴을 채택한 이유**
   - 비동기 처리이므로 즉시 결과를 반환할 수 없음
   - 202 + requestId 반환 → 클라이언트가 GET으로 폴링
   - WebSocket/SSE보다 구현 단순. 쿠폰 발급은 수초 내 완료 → 폴링으로 충분

2. **상태 전이: PENDING → ISSUED / REJECTED**
   - PENDING: API에서 접수 완료, Consumer 미처리
   - ISSUED: Consumer가 UserCoupon 생성 성공
   - REJECTED: Consumer가 거절 (중복, 수량 초과 등)

### 커밋 3-6: CouponIssueConsumer + CouponIssueProcessor (commerce-streamer)

**의사결정:**

1. **JdbcTemplate을 사용하여 cross-app 테이블에 접근한 이유**
   - CouponIssueProcessor는 commerce-streamer에 위치하지만 user_coupons, coupon_issue_requests, coupons 테이블(commerce-api 도메인)에 접근 필요
   - JPA Entity를 commerce-streamer에 중복 정의하면 도메인 모델 중복 + 동기화 부담
   - JdbcTemplate으로 직접 SQL 실행 → 엔티티 중복 없이 필요한 쿼리만 수행

2. **DB COUNT를 ground truth로 사용하는 이유**
   - Redis 카운터는 근사치 (INCR/DECR 보정에도 미세한 불일치 가능)
   - `SELECT COUNT(*) FROM user_coupons WHERE coupon_id = ?`가 실제 발급 수의 진실 공급원
   - Redis 재시작, 카운터 초기화 등 극단적 상황에서도 초과 발급 방지

3. **unique constraint로 중복 발급을 방어하는 이유**
   - 같은 유저가 같은 쿠폰에 대해 여러 CouponIssueRequest를 보낼 수 있음
   - user_coupons 테이블의 (user_id, coupon_id) unique constraint가 최종 방어선
   - DataIntegrityViolationException catch → REJECTED 처리

4. **Consumer groupId를 `commerce-streamer-coupon`으로 분리한 이유**
   - catalog-events, order-events Consumer와 독립적인 오프셋 관리
   - 쿠폰 발급 Consumer 장애가 다른 Consumer에 영향을 주지 않음

**트레이드오프:**

| 결정 | 선택 | 대안 | WHY |
|------|------|------|-----|
| Cross-app 테이블 접근 | JdbcTemplate 직접 SQL | JPA Entity 중복 정의 | 도메인 모델 중복 방지. SQL만으로 충분 |
| 수량 검증 | DB COUNT (ground truth) | Redis 카운터 신뢰 | Redis는 근사치. DB가 진실 공급원 |
| 중복 방어 | DB unique constraint | 애플리케이션 락 | DB 제약이 가장 확실한 방어선 |

### 커밋 3-7/3-8: 동시성 테스트 + Redis 카운터 보정 테스트

**의사결정:**

1. **100명 동시 요청 / 10장 쿠폰 테스트 설계**
   - ConcurrencyTestHelper로 100 스레드 동시 실행
   - Redis INCR 게이트의 원자성 검증: 정확히 10건만 통과
   - DB 검증: PENDING 상태 요청이 정확히 10건
   - Redis 카운터 검증: DECR 보정 후 정확히 10

2. **Redis 카운터 보정 테스트를 별도로 추가한 이유**
   - SOLD_OUT 거절 시 DECR이 정확히 동작하는지 검증
   - 소규모(20 요청/3 수량)로 카운터 정확도만 집중 검증
   - 대규모 테스트에서 놓칠 수 있는 edge case 보완

3. **RequestCouponIssueAsyncUseCase에 try-catch DECR 보정을 추가한 이유**
   - Redis INCR 성공 후 DB 저장 실패(TX 롤백) 시 카운터 과대 계상
   - try-catch로 DB 실패 감지 → DECR → 카운터 정확성 복원
   - 이 보정이 없으면 실제 발급 가능 수량보다 Redis 카운터가 높아져 SOLD_OUT 오보 발생

**트레이드오프:**

| 결정 | 선택 | 대안 | WHY |
|------|------|------|-----|
| 테스트 규모 | 100 스레드 | 1000 스레드 (plan 원안) | 100 스레드로 충분한 동시성 검증. CI 부담 감소 |
| 카운터 보정 범위 | API 레이어 DECR | Consumer DECR까지 | Consumer는 별도 앱(commerce-streamer). API 레이어 보정이 우선 |
| 동일 유저 중복 테스트 | 제외 (API 레이어에서 완전 방어 불가) | 포함 | 비동기 플로우에서 중복 방어는 Consumer 책임. API는 선행 필터링만 |
