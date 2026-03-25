# Round 7 — 이벤트 기반 아키텍처 & Kafka 파이프라인

## 배경

현재 주문-결제 플로우는 재고 차감, 쿠폰 사용, 결제 처리 등 모든 흐름을 하나의 트랜잭션에서 처리한다.
트랜잭션이 커지면서 실패 전파, 높은 결합도, 재시도 불가, 성능 저하 문제가 발생한다.

이벤트 기반 아키텍처를 도입하여 핵심 트랜잭션과 후속 처리를 분리하고,
Kafka를 통해 서비스 경계를 넘는 이벤트 파이프라인을 구축한다.

### 학습 목표

- Spring ApplicationEvent로 유스케이스의 핵심/부가 로직 경계를 판단하고 분리한다.
- Kafka Producer/Consumer 파이프라인을 구축하고 Transactional Outbox Pattern으로 발행을 보장한다.
- Kafka 기반 선착순 쿠폰 발급으로 대량 요청의 동시성 제어를 구현한다.

### 키워드

- ApplicationEventPublisher, @TransactionalEventListener
- 트랜잭션 분리 & 도메인 decoupling
- Kafka Producer / Consumer
- Transactional Outbox Pattern
- At Least Once / Idempotent Consumer
- 선착순 쿠폰 발급

### 우선순위

**Must-Have**

- Event vs Command 구분
- ApplicationEvent 기반 트랜잭션 분리
- Kafka Producer / Consumer 기본 파이프라인
- Transactional Outbox Pattern
- Kafka 기반 선착순 쿠폰 발급

**Nice-to-Have**

- Consumer Group 분리를 통한 관심사별 처리
- Consumer 배치 처리
- DLQ 구성

---

## 1. 문제 정의

### 핵심 목표

- 핵심 트랜잭션(주문 생성)과 후속 처리(쿠폰 차감, 포인트 적립, 알림)를 분리하여 장애 격리
- 유저 행동(조회, 좋아요, 주문 등)을 이벤트로 발행하여 메트릭스 집계 파이프라인 구축
- Outbox Pattern으로 이벤트 유실 없는 At Least Once 발행 보장
- Kafka 기반 선착순 쿠폰 발급으로 대량 동시 요청을 안전하게 처리

### 이벤트 기반 트랜잭션 분리

| 관점 | 문제 |
|------|------|
| 사용자 | PG 장애·지연 시 주문 자체가 실패하여 구매 경험이 끊김 |
| 비즈니스 | 부가 로직 실패가 핵심 트랜잭션 성공률에 영향을 줌 |
| 시스템 | 긴 트랜잭션으로 인한 DB 락 경합·TPS 저하, 도메인 간 강결합 |

### Kafka 이벤트 파이프라인

| 관점 | 문제 |
|------|------|
| 사용자 | 상품 메트릭스(좋아요 수, 판매량, 조회 수)가 실시간에 가깝게 반영되길 기대 |
| 비즈니스 | 서비스 경계 간 느슨한 결합으로 독립 배포·스케일링 가능한 구조 필요 |
| 시스템 | 이벤트 유실 방지, 멱등 소비, 메트릭스 집계 파이프라인 구축 |

### 선착순 쿠폰 발급

| 관점 | 문제 |
|------|------|
| 사용자 | 대량 동시 요청에서도 공정하게 선착순으로 쿠폰을 발급받길 기대 |
| 비즈니스 | 선착순 N장 한정 마케팅 이벤트를 수량 초과 없이 안전하게 운영 |
| 시스템 | Kafka로 요청을 버퍼링·순차 처리하여 동시성 제어, 중복 발급 방지 |

---

## 2. 유비쿼터스 언어

| 한글 | 영문 | 정의 |
|------|------|------|
| 도메인 이벤트 | Domain Event | 도메인에서 발생한 사실을 나타내는 불변 객체 |
| 애플리케이션 이벤트 | Application Event | Spring ApplicationEventPublisher를 통해 JVM 내부에서 전파되는 이벤트 |
| 아웃박스 | Outbox | 도메인 변경과 동일 트랜잭션으로 기록되는 이벤트 저장 테이블 |
| 릴레이 | Relay | Outbox 테이블의 미발행 이벤트를 Kafka로 전달하는 배치/폴링 컴포넌트 |
| 멱등 처리 | Idempotent Processing | 같은 이벤트가 여러 번 도착해도 최종 결과가 동일하도록 보장하는 처리 방식 |
| 상품 메트릭스 | Product Metrics | 좋아요 수, 판매량, 조회 수 등을 집계하는 별도 읽기 모델 |
| 선착순 쿠폰 | FCFS Coupon | 선착순(First-Come First-Served) 수량 제한 쿠폰 |
| 발급 요청 | Issue Request | 쿠폰 발급을 위한 비동기 요청. Kafka에 발행되어 Consumer가 처리 |
| 컨슈머 그룹 | Consumer Group | 동일 토픽을 구독하는 컨슈머 집합. 파티션을 분배하여 병렬 소비 |
| 이벤트 핸들링 | Event Handled | 멱등 처리를 위해 처리 완료된 이벤트 ID를 기록하는 테이블 |
| DLQ | Dead Letter Queue | 반복 실패한 메시지를 격리하여 운영자가 후처리하는 큐 |

---

## 3. 액터 정의

| 액터 | 식별 방식 | 권한 및 역할 |
|------|----------|------------|
| 인증된 사용자 | JWT 토큰 (userId) | 상품 조회, 좋아요, 주문, 쿠폰 발급 요청, 발급 결과 조회 |
| 관리자 | JWT 토큰 (role=ADMIN) | 쿠폰 생성·관리, 메트릭스 조회 |
| commerce-api | 애플리케이션 | 이벤트 발행, Outbox 기록, Kafka Producer |
| commerce-streamer | 애플리케이션 | Kafka Consumer, 메트릭스 집계, 쿠폰 발급 처리 |
| Outbox Relay | 내부 컴포넌트 | Outbox 테이블 폴링 → Kafka 발행 |

---

## 4. 유저 시나리오

### 4.1 이벤트 기반 주문-결제 분리 (인증된 사용자)

**사전 조건:** 사용자가 인증됨. 주문이 생성되어 있음.

**주문 생성 (핵심 트랜잭션):**
- 재고 차감 + 주문 저장은 동기 트랜잭션으로 처리 (기존과 동일)
- 주문 생성 커밋 후 ApplicationEvent 발행

**후속 처리 (이벤트 기반):**
- 결제 후 쿠폰 차감: @TransactionalEventListener(AFTER_COMMIT)
- 포인트 적립 기록: @TransactionalEventListener(AFTER_COMMIT)
- 유저 행동 로깅 (주문 완료): 이벤트 기반 비동기 처리

**예외 흐름:**

| 조건 | 응답 | 설명 |
|------|------|------|
| 후속 처리 실패 | 핵심 트랜잭션은 성공 유지 | 후속 실패는 로그 기록, 재시도 또는 보상 처리 |

### 4.2 유저 행동 이벤트 발행 (인증된 사용자)

**사전 조건:** 사용자가 인증됨.

**유저 행동 발생 시:**
- 상품 상세 조회, 좋아요 추가/삭제, 주문 완료 등의 행동이 ApplicationEvent로 발행
- 이벤트는 Outbox 테이블에 기록 → Relay가 Kafka로 발행
- commerce-streamer가 소비하여 product_metrics에 upsert

**좋아요 처리:**
- 좋아요 추가/삭제: 동기 처리 (Product.likeCount 즉시 반영)
- 집계(product_metrics): 비동기 처리 (Kafka → commerce-streamer)

### 4.3 선착순 쿠폰 발급 (인증된 사용자)

**사전 조건:** 사용자가 인증됨. 선착순 쿠폰이 활성 상태.

**발급 요청:**
- `POST /coupons/issue` → Kafka `coupon-issue-requests` 토픽에 발행
- API는 requestId를 즉시 반환 (비동기 처리)

**Consumer 처리:**
- commerce-streamer가 순차적으로 소비
- 수량 확인 → 중복 발급 방지 (userId 기반) → 발급 or 거절
- 발급 결과 저장

**결과 조회:**
- `GET /coupons/issue/{requestId}` → Polling으로 결과 확인

**예외 흐름:**

| 조건 | 응답 | 설명 |
|------|------|------|
| 수량 소진 | 발급 실패 (SOLD_OUT) | 잔여 수량 0일 때 |
| 중복 요청 | 발급 실패 (DUPLICATE) | 동일 userId로 이미 발급됨 |
| 쿠폰 비활성 | 400 BAD_REQUEST | 발급 기간이 아니거나 비활성 쿠폰 |

---

## 5. 도메인 규칙 (Business Rules)

### 이벤트 분리 기준

| 구분 | 처리 방식 | 트랜잭션 경계 |
|------|----------|------------|
| 핵심 트랜잭션 | 주문 생성, 재고 차감, 유효성 검증 | 동기, 커밋 보장 |
| 후속 트랜잭션 | 쿠폰 차감, 포인트 적립, 알림 | 커밋 이후 이벤트 기반 처리 |
| 시스템 간 전파 | 메트릭스 집계, 유저 행동 로깅 | Outbox → Kafka → Consumer |

### Outbox 규칙

- 도메인 데이터 변경과 Outbox 기록은 하나의 DB 트랜잭션으로 묶는다.
- 도메인별 Outbox 테이블을 사용한다 (catalog_outbox, order_outbox, coupon_outbox).
- Relay가 미발행 이벤트를 폴링하여 Kafka로 발행한다.
- 발행 성공 시 Outbox 레코드를 발행 완료로 마킹한다.

### 멱등 처리 규칙

- Consumer는 `event_handled(event_id PK)` 테이블로 중복 처리를 방지한다.
- 이미 처리된 이벤트는 skip한다.
- `version` 또는 `updated_at` 기준으로 최신 이벤트만 반영한다.

### 선착순 쿠폰 발급 규칙

- 기존 IssueCouponUseCase를 Kafka 기반으로 교체한다.
- 발급 수량은 쿠폰 정의에 설정된 총 수량을 초과할 수 없다.
- 동일 사용자에게 같은 쿠폰을 중복 발급하지 않는다.
- Consumer가 순차 처리하므로 파티션 단위로 순서가 보장된다.

### product_metrics 규칙

- Product 테이블의 likeCount와 별개의 읽기 모델이다.
- 집계 대상: 좋아요 수, 판매량, 상세 페이지 조회 수.
- Kafka 이벤트를 소비하여 upsert로 집계한다.

---

## 6. API 명세

### 6.1 선착순 쿠폰 발급

| METHOD | URI | 인증 | 설명 |
|--------|-----|------|------|
| POST | /coupons/issue | 필수 | 쿠폰 발급 요청 (Kafka 발행, 비동기) |
| GET | /coupons/issue/{requestId} | 필수 | 발급 결과 조회 (Polling) |

### 6.2 기존 API 변경사항

기존 API의 엔드포인트·시그니처는 변경하지 않되, 내부 동작이 이벤트 기반으로 변경된다:
- 주문-결제 플로우: 후속 처리가 ApplicationEvent로 분리
- 좋아요: 추가/삭제는 동기 유지, 집계가 비동기로 분리
- 상품 조회: 조회 이벤트 발행 추가

---

## 7. 인증/인가

기존 인증 체계(JWT) 동일. 변경 없음.

| 경로 | 인증 요구 |
|------|----------|
| POST /coupons/issue | 인증 필수 |
| GET /coupons/issue/{requestId} | 인증 필수 (본인 요청만 조회) |

---

## 8. 기존 시스템과의 관계

### 기존 완료 (재사용)

- **commerce-api**: 주문(PlaceOrderUseCase), 결제(RequestPaymentUseCase), 좋아요(AddLikeUseCase, RemoveLikeUseCase), 쿠폰(IssueCouponUseCase) 등 핵심 도메인
- **commerce-streamer**: 스켈레톤 앱 존재 (DemoKafkaConsumer만 있음)
- **modules/kafka**: Kafka 인프라 모듈 이미 구성됨
- **ProductCacheEvent**: ApplicationEventPublisher 기반 캐시 무효화 패턴 (참고 가능)

### 신규 구현

- **ApplicationEvent 이벤트 클래스**: 주문 완료, 결제 완료, 좋아요, 상품 조회 등
- **@TransactionalEventListener 핸들러**: 후속 처리 (쿠폰 차감, 포인트 적립 등)
- **도메인별 Outbox 테이블 + Entity**: catalog_outbox, order_outbox, coupon_outbox
- **Outbox Relay**: Outbox → Kafka 발행 컴포넌트
- **Kafka Producer 설정**: acks=all, idempotence=true
- **Kafka Consumer (commerce-streamer)**: 메트릭스 집계, 쿠폰 발급 처리
- **product_metrics 테이블 + Entity**: 별도 읽기 모델
- **event_handled 테이블**: 멱등 처리용
- **선착순 쿠폰 발급 API**: POST /coupons/issue, GET /coupons/issue/{requestId}
- **유저 행동 로깅**: 조회, 좋아요, 주문 등의 서버 레벨 이벤트

### 추후 확장

- Consumer Group 분리를 통한 관심사별 처리 (Nice-to-Have)
- Consumer 배치 처리 (Nice-to-Have)
- DLQ 구성 (Nice-to-Have)

---

## 9. 잠재 리스크

| 리스크 | 영향 | 현재 대응 | 향후 대응 |
|--------|------|----------|----------|
| 후속 이벤트 처리 실패 | 쿠폰 차감·포인트 적립 누락 | 로그 기록, Outbox 재시도 | DLQ 격리 + 운영 대시보드 |
| Outbox Relay 지연 | Kafka 발행 지연 → 메트릭스 반영 지연 | 폴링 주기 조절 | CDC(Debezium) 도입 검토 |
| Consumer 장애 | 메트릭스 집계·쿠폰 발급 중단 | Kafka에 메시지 보존, 복구 후 재처리 | Consumer 이중화 |
| 중복 이벤트 발행 | 멱등 처리 안 되면 중복 집계·중복 발급 | event_handled 테이블 기반 멱등 처리 | — |
| 선착순 수량 초과 | 쿠폰 초과 발급 | 단일 파티션 순차 처리로 동시성 제어 | 분산 락 or Redis 원자 연산 보완 |
| Outbox 테이블 증가 | 디스크·쿼리 성능 영향 | 발행 완료 레코드 주기적 정리 | 보존 기간 정책 수립 |

---

## 10. 설계 결정 사항

### commerce-streamer 명칭 유지

- 요구사항에서 `commerce-collector`로 언급되었으나, 기존 모듈 `commerce-streamer`를 그대로 사용한다.
- 이유: 코드 변경 최소화, 기존 모듈 활용.

### 도메인별 Outbox 테이블

- 범용 Outbox (JSON payload) 대신 도메인별 Outbox 테이블(catalog_outbox, order_outbox, coupon_outbox)을 사용한다.
- 이유: JSON 필드는 인덱스를 탈 수 없어 조건 검색 시 한계가 있음. 도메인별 테이블이면 각 컬럼에 적절한 인덱스를 적용할 수 있음.
- 트레이드오프: 테이블 수 증가 → 현재 토픽 3개 수준이므로 관리 가능.

### 좋아요 동기/비동기 분리

- 좋아요 추가/삭제는 동기 처리 (Product.likeCount 즉시 반영).
- 집계(product_metrics)만 비동기 처리 (Kafka → commerce-streamer).
- 이유: 사용자에게 좋아요 반영은 즉시 보여야 하므로 동기 유지. 메트릭스는 eventual consistency 허용.

### 기존 IssueCouponUseCase 교체

- 기존 동기 발급 방식을 Kafka 기반 비동기 발급으로 교체한다.
- API는 발급 요청을 Kafka에 발행하고 requestId를 반환. Consumer가 실제 발급.
- 발급 결과는 Polling(`GET /coupons/issue/{requestId}`)으로 확인.

### 모든 Kafka 발행은 Outbox 경유

- 선착순 쿠폰 포함, 모든 이벤트를 Outbox 패턴으로 발행한다.
- 직접 Kafka 발행 시 실패하면 PENDING 고아 레코드가 발생하므로, Outbox로 At Least Once 보장.
- Relay 폴링 주기를 짧게 설정하여 선착순 쿠폰의 지연을 최소화한다.

### Kafka Producer 설정

- `acks=all`: 모든 replica에 기록 확인 후 발행 완료 처리.
- `idempotence=true`: 네트워크 재시도 시 중복 발행 방지.
- Partition Key: aggregateId(productId, orderId, couponId)로 순서 보장.

### Kafka Consumer 설정

- manual Ack: 처리 완료 후 명시적으로 offset commit.
- `event_handled` 테이블: 멱등 처리를 위한 처리 완료 이벤트 ID 기록.
- `version`/`updated_at` 기준: 최신 이벤트만 반영, 오래된 이벤트는 무시.

---

## 토픽 설계

| 토픽명 | Partition Key | 발행 주체 | 소비 주체 | 이벤트 |
|--------|-------------|----------|----------|--------|
| catalog-events | productId | commerce-api | commerce-streamer | 상품 조회, 좋아요 추가/삭제 |
| order-events | orderId | commerce-api | commerce-streamer | 주문 완료, 결제 완료 |
| coupon-issue-requests | couponId | commerce-api | commerce-streamer | 쿠폰 발급 요청 |

---

## Step별 구현 범위

### Step 1 — ApplicationEvent로 경계 나누기

- [ ] 주문-결제 플로우에서 부가 로직(쿠폰 차감, 포인트 적립, 알림)을 이벤트 기반으로 분리
- [ ] 좋아요 추가/삭제는 동기 유지, 집계(product_metrics)를 이벤트 기반으로 분리
- [ ] 유저 행동(조회, 좋아요, 주문 등)에 대한 서버 레벨 로깅을 이벤트로 처리
- [ ] @TransactionalEventListener의 phase를 트랜잭션 연관관계에 따라 적절히 선택

### Step 2 — Kafka Producer / Consumer

- [ ] Step 1의 ApplicationEvent 중 시스템 간 전파가 필요한 이벤트를 Kafka로 발행
- [ ] acks=all, idempotence=true 설정
- [ ] 도메인별 Outbox 테이블 + Relay로 Transactional Outbox Pattern 구현
- [ ] PartitionKey 기반 이벤트 순서 보장
- [ ] commerce-streamer에서 Metrics 집계 처리 (product_metrics upsert)
- [ ] event_handled 테이블을 통한 멱등 처리 구현
- [ ] manual Ack + version/updated_at 기준 최신 이벤트만 반영

### Step 3 — 선착순 쿠폰 발급

- [ ] 기존 IssueCouponUseCase를 Kafka 기반 비동기 발급으로 교체
- [ ] POST /coupons/issue → Kafka coupon-issue-requests 발행
- [ ] commerce-streamer Consumer에서 선착순 수량 제한 + 중복 발급 방지 구현
- [ ] GET /coupons/issue/{requestId} — Polling으로 발급 결과 조회
- [ ] 동시성 테스트 — 수량 초과 발급이 발생하지 않는지 검증
