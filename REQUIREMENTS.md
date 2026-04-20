# Requirements

감성 이커머스 서비스의 기능 요구사항 및 대규모 트래픽 대응 성능 개선 요구사항 목록입니다.

---

## 프로젝트 개요

좋아요 누르고, 쿠폰 쓰고, 주문 및 결제하는 감성 이커머스.
내가 좋아하는 브랜드의 상품들을 한 번에 담아 주문하고, 유저 행동은 랭킹과 추천으로 연결됩니다.

### 서비스 흐름

1. 사용자가 **회원가입**을 하고
2. 여러 브랜드의 **상품**을 둘러보고, 마음에 드는 상품엔 **좋아요**를 누르고
3. **쿠폰을 발급**받고, 여러 상품을 **한 번에 주문하고 결제**하며
4. 유저의 행동은 모두 기록되고, **랭킹과 추천**으로 확장됩니다.

### 설계 원칙

- 추후 기능 추가/확장에 유연한 구조
- 새 도메인 추가 시 example 패키지의 4-layer 구조를 따름
- 도메인 간 결합 최소화 (향후 서비스 분리 대비)

---

## 기능 요구사항

### 회원 관리 — DONE

#### FEAT-1: 회원가입

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 필요 정보 | 로그인 ID, 비밀번호, 이름, 생년월일, 이메일 |
| 비즈니스 규칙 | - 이미 가입된 로그인 ID로는 가입 불가 |
|  | - 각 정보는 포맷에 맞는 검증 필요 (이름, 이메일, 생년월일) |
|  | - 비밀번호는 암호화(BCrypt)하여 저장 |
| 비밀번호 규칙 | - 8~16자의 영문 대소문자, 숫자, 특수문자만 가능 |
|  | - 생년월일은 비밀번호 내에 포함 불가 (YYYYMMDD, YYMMDD, MMDD) |
| 로그인 ID 규칙 | 영문과 숫자만 허용 |
| 응답 | 201 CREATED + 회원 정보 (마스킹 이름 포함) |
| 중복 시 | 409 CONFLICT |

#### FEAT-2: 내 정보 조회

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 인증 | `@MemberAuthenticated` + `AuthenticatedMember` (Interceptor가 `X-Loopers-LoginId`/`X-Loopers-LoginPw` 헤더 검증) |
| 반환 정보 | 로그인 ID, 이름(마스킹), 생년월일, 이메일 |
| 이름 마스킹 규칙 | 마지막 글자를 `*`로 마스킹 (예: 홍길동 → 홍길*, 홍길 → 홍*, 김 → *) |
| 응답 | 200 OK + 회원 정보 |
| 인증 실패 시 | 401 UNAUTHORIZED |

#### FEAT-3: 비밀번호 수정

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 인증 | `@MemberAuthenticated` + `AuthenticatedMember` (Interceptor가 기존 비밀번호 검증을 대행) |
| 필요 정보 | 새 비밀번호 (기존 비밀번호는 인증 헤더로 Interceptor에서 검증) |
| 비즈니스 규칙 | - 비밀번호 RULE을 따름 |
|  | - 현재 비밀번호와 동일한 비밀번호로 변경 불가 |
|  | - 변경 시 해당 loginId의 인증 캐시(Redis) eviction |
| 응답 | 200 OK + 회원 정보 |
| 인증 실패 시 | 401 UNAUTHORIZED (Interceptor에서 차단) |

### 브랜드 — DONE

#### FEAT-4: 브랜드 관리

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 대고객 API | `GET /api/v1/brands/{brandId}` (비인증) |
| 어드민 API | CRUD: `GET/POST/PUT/DELETE /api-admin/v1/brands` (LDAP 인증) |
| 비즈니스 규칙 | BR-B1: 브랜드 삭제 시 하위 상품 소프트 삭제 캐스케이드 |
| | BR-B2: 브랜드명 중복 허용 (soft delete + UNIQUE 충돌 방지. 브랜드 식별은 PK 기반) |
| 고객/어드민 정보 분리 | 고객: id, name, description, imageUrl |
| | 어드민: + status, createdAt, updatedAt |
| Soft Delete | status=DELETED + deleted_at 병행 |

### 상품 — DONE

#### FEAT-5: 상품 관리

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 대고객 API | `GET /api/v1/products` (브랜드 필터, 정렬, 페이징), `GET /api/v1/products/{productId}` |
| 어드민 API | CRUD: `GET/POST/PUT/DELETE /api-admin/v1/products` (LDAP 인증) |
| 정렬 옵션 | latest (기본), price_asc, likes_desc |
| 비즈니스 규칙 | BR-P1: 상품 등록 시 브랜드 존재 필수 (Facade에서 검증) |
| | BR-P2: 브랜드는 등록 후 수정 불가 |
| | BR-P3: 가격 0 이상, BR-P4: 재고 0 이상 |
| 고객/어드민 정보 분리 | 고객: id, brandId/brandName, name, description, price, imageUrl, likeCount, soldOut |
| | 어드민: + stockQuantity, status, createdAt, updatedAt |
| 재고 검증 | ProductModel.deductStock()이 불변식 보호 (도메인 모델 내부) |
| 대고객 페이징 | 커서 기반 페이징 (Base64 인코딩). 어드민은 offset 페이징 |
| 좋아요 수 조회 | product.like_count 컬럼 직접 반환 (배치 갱신) |
| 정렬 타입 안전 | ProductSortRequest enum (Presentation) → ProductSort enum (Domain). case-insensitive Converter 적용 |
| 커서 페이징 구현 | QueryDSL 동적 쿼리 (ProductQueryDslRepository). JPA/QueryDSL Repository 분리 |

### 좋아요 — DONE

#### FEAT-6: 상품 좋아요

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 대고객 API | `POST/DELETE /api/v1/products/{productId}/likes` (인증), `GET /api/v1/likes` (인증) |
| 비즈니스 규칙 | BR-L1: 중복 좋아요 불가 (UNIQUE Constraint) |
| | BR-L2: 좋아요 등록/취소 양방향 멱등 (이미 좋아요→성공, 좋아요 없이 취소→성공) |
| | BR-L3: 본인 좋아요 목록만 조회 가능 (인증된 사용자 기준, URL에 userId 미포함) |
| | BR-L4: 존재하지 않는 상품에 좋아요 불가 (Facade에서 ProductService로 검증) |
| 멱등 전략 | Facade에서 exists 사전 조회(200 멱등) + UNIQUE Constraint 안전망(race condition → 409). @Transactional은 Facade 주도 (D33, D36) |
| 집계 전략 | product.like_count 컬럼 (DEFAULT 0) + commerce-batch 배치 갱신 (5분 주기, 대규모 트래픽 대응) |
| 페이징 | 좋아요 목록 조회에 페이징 없음 (API 명세대로) |

### 주문 — DONE

#### FEAT-7: 주문

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 대고객 API | `POST /api/v1/orders` (인증), `GET /api/v1/orders?startAt&endAt` (인증), `GET /api/v1/orders/{orderId}` (인증) |
| 어드민 API | `GET /api-admin/v1/orders` (페이징, LDAP), `GET /api-admin/v1/orders/{orderId}` (LDAP) |
| 비즈니스 규칙 | BR-O1: 재고 확인 후 차감 (ProductModel.deductStock 불변식 보호) |
| | BR-O2: 주문 시점 상품 스냅샷 저장 (productName, productPrice, brandName만 복사) |
| | BR-O3: 최소 1개 상품, BR-O4: 수량 1 이상 |
| | BR-O5: 본인 주문만 조회 (OrderModel.validateOwner 도메인 검증) |
| | BR-O6: 재고 부족 시 주문 실패 |
| 주문 총액 | `getOriginalAmount() = items.sumOf { it.amount }`, `getTotalAmount() = originalAmount - discountAmount` |
| 쿠폰 연동 | couponId(nullable), discountAmount 스냅샷 저장. 쿠폰 검증 후 할인 적용 |
| 스냅샷 범위 | productName, productPrice, brandName (imageUrl, description 제외) |
| OrderItem 필드 역할 | 참조(productId), 스냅샷(Name/Price/Brand), 주문입력(quantity), 파생(amount) |
| 주문 번호 | UUID (내부 ID 노출 방지) |
| 주문 상태 | ORDERED / CANCELLED (주문 취소 API는 현재 스코프 외) |
| 트랜잭션 | Facade @Transactional로 재고 차감 + 주문 생성 원자성 보장 |
| 동시성 | Phase 1에 비관적 락(SELECT FOR UPDATE) 포함. 재고 차감 시 정합성 보장 |
| 대고객 페이징 | 기간 필터만 적용, 페이징 없음 (API 명세대로) |

### 쿠폰 — DONE

#### FEAT-8: 쿠폰

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 대고객 API | `POST /api/v1/coupons/templates/{templateId}/issue` (발급, 인증), `GET /api/v1/members/me/coupons` (내 쿠폰 목록, 인증) |
| 어드민 API | CRUD: `GET/POST/PUT/DELETE /api-admin/v1/coupons` (LDAP), `GET /api-admin/v1/coupons/{couponId}/issues` (발급 내역) |
| 비즈니스 규칙 | BR-C1: 쿠폰 타입 — FIXED(정액), RATE(정률, maxDiscountAmount 한도) |
| | BR-C2: 만료 정책 — FIXED_DATE(특정일), DAYS_FROM_ISSUE(발급일+N일) |
| | BR-C3: 발급 쿠폰에 개별 expiredAt 저장 (만료 정책에 따라 계산) |
| | BR-C4: 중복 발급 허용 (동일 템플릿 여러 장 발급 가능) |
| | BR-C5: 삭제된 템플릿 쿠폰 — 이미 발급된 쿠폰은 만료일까지 사용 가능 |
| | BR-C6: 표시 상태 — DB(AVAILABLE/USED) + 조회 시 만료 판단(EXPIRED) |
| | BR-C7: 쿠폰 동시 사용 방지 — 낙관적 락(@Version), 충돌 시 409 CONFLICT |
| | BR-C8: 최소 주문 금액 검증 (minOrderAmount) |
| 주문 연동 | BR-C9: 주문 시 쿠폰 할인 적용 (originalAmount, discountAmount, totalAmount 스냅샷) |
| | BR-C10: 쿠폰 검증 순서 — 소유자 → 사용 가능 상태 → 만료 → 최소 주문 금액 → 할인 계산 |
| | BR-C11: 쿠폰 사용 실패 시 전체 트랜잭션 롤백 (재고 복원) |
| 동시성 | 재고: 비관적 락(SELECT FOR UPDATE), 데드락 방지(productId 정렬) |
| | 쿠폰: 낙관적 락(@Version), flush()로 조기 충돌 감지, 1건만 성공, 나머지 409 |
| | 좋아요: 멱등(모두 200), DataIntegrityViolation catch |
| 주문 플로우 최적화 | 쿠폰 차감(@Version + flush)을 재고 락(SELECT FOR UPDATE) 전에 실행하여 불필요한 락 점유 방지 (D34) |
| 락 비교 테스트 | 비관적/낙관적 락 동일 시나리오 비교 테스트 — 단일 자원 경합, 초과 경합 (D35) |
| Soft Delete | 템플릿: status=DELETED + deleted_at |
| DDL | `docs/ddl/V4A__create_coupon.sql`, `docs/ddl/V4B__alter_orders_add_coupon.sql` |

### 결제 — DONE

#### FEAT-9: 결제

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 배경 | 주문 생성 후 외부 PG를 통한 비동기 결제 처리 필요 |
| 기능 범위 | 결제 요청, PG 콜백 수신, 폴링 기반 미완료 건 처리, 결제 실패 보상 트랜잭션 |
| 수용 기준 | 결제 요청 API, 콜백 수신, 30초 주기 폴링(60초 이상 PENDING 건), 실패 시 재고+쿠폰 복원 |
| 제약사항 | PG 호출은 트랜잭션 밖에서 수행 (D46). 보상 트랜잭션 시 재고복원 productId 오름차순 (D49) |
| 관련 결정 | D43 (PgClient 포트), D44 (콜백+폴링 이중화), D45 (결제 상태 머신), D46 (트랜잭션 경계), D47 (PG 실패 유형), D48 (서킷브레이커), D49 (보상 트랜잭션), D50 (주문 상태 확장), D51 (CardNo VO), D52 (Event-Command-Handler) |

### 선착순 쿠폰 — DONE

#### FEAT-11: 선착순 쿠폰 발급

**배경**: 한정 수량 쿠폰을 선착순으로 발급해야 한다. 동시 요청 시 수량 초과 발급을 방지해야 한다.

**수용 기준**:
- 쿠폰 발급 요청 API → Kafka 발행 (비동기 처리, 202 Accepted)
- Consumer에서 선착순 수량 제한 + 중복 발급 방지
- 발급 완료/실패 결과를 폴링 API로 확인 가능
- 동시 요청 시 수량 초과 발급 불가

**제약사항**: 기존 쿠폰 시스템(CouponTemplate)과 별도 도메인(FcfsCouponTemplate)으로 구현

**관련 결정**: D55

**상태**: DONE

### 대기열 — DONE

#### FEAT-12: 주문 대기열 시스템

**배경**: 블랙 프라이데이 등 대규모 트래픽이 주문 API에 직접 유입되면 DB 커넥션 풀(50개) 고갈 및 전체 서비스 장애 위험. 선착순 공정성 보장과 DB 보호를 위해 가상 대기열 도입.

**수용 기준**:
- Redis Sorted Set 기반 놀이공원식(non-blocking) 대기열
- 대기열 진입(POST /enter) → 순번 조회 Polling(GET /position) → 토큰 발급 → 주문 진입
- 스케줄러 3초 주기 batchSize=300명 입장 (DB Pool 역산)
- 토큰 기반 주문 게이트 (@QueueTokenRequired + Interceptor)
- fail-closed: Redis 장애 시 503 반환 (DB 보호 우선)
- 어드민 수동 토글 ON/OFF
- 분산 락 스케줄러 (SET NX EX + Lua 소유자 해제)
- 동적 retryAfter (position 구간별 2/5/10초)

**제약사항**: queue 독립 도메인 (order와 분리), 토큰 TTL 5분, AFTER_COMMIT으로 토큰 삭제

**관련 결정**: D59 (CoreException 핸들러), D60 (activeCount Pipeline), D61 (부하테스트 역산)

**k6 검증**: Entry 1K RPS p99=15ms, Polling 2K RPS p99=12ms, Order 100 TPS p99=95ms (SLO p99≤500ms 충족)

**상태**: DONE

### 이벤트 아키텍처 — DONE

#### ARCH-1: Event-Command-Handler 도메인 디커플링

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 배경 | Facade가 다른 도메인 Service를 직접 호출하여 강한 결합 존재. MSA 전환 대비 논리적 분리 필요 |
| 기능 범위 | 7개 도메인 이벤트 발행(22개), Command 정의(11개), EventHandler/CommandHandler, Kafka 프로듀서/컨슈머, Polling 보정 배치 |
| 수용 기준 | 도메인 간 직접 Service 호출 제거, 이벤트 발행/수신 단위 테스트 통과, 전체 컴파일 성공 |
| 제약사항 | DIP 유지 (Service에서 ApplicationEventPublisher 사용), EventHandler는 모두 AFTER_COMMIT, 결제 보상은 1차(Kafka)만 구현 |
| 관련 결정 | D52 (Event-Command-Handler 아키텍처) |

#### ARCH-2: Transactional Outbox Pattern

**배경**: ApplicationEvent 기반 이벤트는 프로세스 내부 전파만 가능. Kafka 전파 시 유실 방지 필요.

**수용 기준**:
- outbox_event 테이블에 비즈니스 TX와 함께 INSERT
- OutboxPoller(commerce-streamer)가 1초 주기로 poll → Kafka 발행
- kafka_consumed_event 테이블로 Consumer 멱등 처리
- 기존 LocalPublisher → OutboxPublisher 교체

**관련 결정**: D54

**상태**: DONE

#### ARCH-3: 유저 행동 로깅

**배경**: 유저 행동(조회, 좋아요, 주문)에 대한 서버 레벨 로깅 필요.

**수용 기준**:
- AOP(@LogUserAction)로 Facade 메서드에 어노테이션
- user_action_log 테이블에 행동 로그 저장
- Outbox → Kafka → product_metrics 집계 파이프라인

**관련 결정**: D53

**상태**: DONE

### 랭킹/추천 — DONE

#### FEAT-10: Redis ZSET 기반 실시간 랭킹 시스템

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 도메인 | ranking |
| 관련 결정 | D62, D63, D64, D65, D66, D67, D68 |

**배경**:
유저 행동(VIEW, LIKE, ORDER) 이벤트를 기반으로 상품 실시간 랭킹을 집계하고 API로 노출한다.
기존 UserActionEvent → Outbox → Kafka 파이프라인을 재활용하여 별도 인프라 추가 없이 구현한다.

**수용 기준**:
- GET /api/v1/rankings?date=yyyyMMdd&size=20&page=0 — 일간 랭킹 조회
- GET /api/v1/rankings/hourly?date=yyyyMMdd&hour=HH&size=20&page=0 — 시간별 랭킹 조회
- GET /api/v1/products/{id} 응답에 rank: Int? 포함 (일간 랭킹 기준 순위)
- RankingScoreConsumer (commerce-streamer): BATCH_LISTENER, 별도 consumer group
- 가중치: VIEW=0.1, LIKE=0.2, ORDER=0.7×log10(price×quantity+1)
- 키 전략: ranking:all:{yyyyMMdd} (TTL 2일), ranking:all:{yyyyMMdd}:{HH} (TTL 3시간)
- 콜드 스타트 완화: RankingCarryOverJob (매일 00:05) — 전일 10% ZUNIONSTORE 이월
- 4-layer 구조: RankingEntry, RankingStore, RedisRankingStoreImpl, RankingService, RankingFacade, RankingV1Controller

**제약사항**:
- 멱등성 미적용 (at-least-once 허용) — 랭킹 특성상 소폭 중복 가산 수용
- ORDER 이벤트는 AOP 제외, OrderFacade에서 수동 발행 (D68)

#### FEAT-13: Spring Batch 주간/월간 랭킹 집계 + MV 적재

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 도메인 | ranking / batch |
| 관련 결정 | D69, D70, D71, D72, D73, D74, D75 |

**배경**:
FEAT-10(실시간 일간 랭킹)의 확장. 대규모 집계 + 조회 전용 구조(Materialized View) 패턴을 Spring Batch로 학습하면서, API를 일간/주간/월간으로 확장한다. 주간/월간은 실시간성보다 정확성·효율성이 중요하므로 Redis가 아닌 DB 기반 MV로 제공한다.

**수용 기준**:
- 새 테이블 `product_metrics_daily (product_id, metric_date) PK` — Kafka Consumer가 실시간 UPSERT (D69)
- Materialized View: `mv_product_rank_weekly`, `mv_product_rank_monthly` — Spring Batch Job이 TOP 200 적재, PK `(period_key, product_id)`, UK `(period_key, rank_value)`
- 2-Step Chunk+Tasklet 구조 (D70): Step1 JdbcCursorItemReader→ScoreProcessor→JdbcBatchItemWriter(rank_staging), Step2 Tasklet(staging SELECT → rank 부여 → MV UPSERT → cleanup)
- chunkSize=500, `INSERT…ON DUPLICATE KEY UPDATE` 기반 멱등 재실행
- 점수 공식: `VIEW*0.1 + LIKE*0.2 + 0.7*log10(SUM(order_amount_sum)+1)` — 원시값 보존으로 가중치 튜닝 가능 (D71)
- 기간 경계: ISO 8601 주 (월~일) + 달력월 (1일~말일), Asia/Seoul 고정 (D72)
- API 확장: `GET /api/v1/rankings?period=daily|weekly|monthly&date=yyyyMMdd&size&page` — 단일 엔드포인트, `/hourly` 별도 유지, 잘못된 period는 400 (D73)
- `ProductMetricsDailyConsumer` (commerce-streamer): 독립 Consumer Group `commerce-streamer-metrics-daily`, BATCH_LISTENER + 인메모리 Map 집계
- metric_date는 `record.timestamp()` 기반 event-time, 누락 시 KST now fallback (D74)
- event-id 기반 멱등성 (`kafka_consumed_event INSERT IGNORE`) — Kafka 재전달 시 중복 가산 차단 (D74)
- Scheduler (`@Scheduled cron`): 주간 `0 10 0 ? * MON` Asia/Seoul, 월간 `0 20 0 1 * ?` Asia/Seoul — `ranking.scheduler.enabled=true` 일 때만 활성 (D75)
- Job Config는 항상 Bean 등록, CLI 단일 실행 시 `spring.batch.job.names=…` 로 선택 실행 (D75)

**제약사항**:
- DB 기반 MV → 실시간성 부족 (주간은 매주 월 00:10, 월간은 매월 1일 00:20 갱신)
- 점수 공식: Redis 일간(이벤트별 log10 합산)과 Batch 주간/월간(기간 합산 후 log10)은 수학적으로 상이 — 채계 차이 문서화 필요 (D71)
- commerce-streamer 의 `ProductMetricsDailyConsumer`는 event-id 멱등성 적용 (D65 랭킹 at-least-once 정책과 별개 — DB 영속 특성상 정확성 우선, D74)
- 다중 인스턴스 스케줄러 동시 실행 방지는 Spring Batch `JobRepository`의 identical JobParameters 차단에 1차 의존. ShedLock은 후속 작업

---

## 성능 목표

- **목표**: 대규모 커머스 서비스 수준의 피크 트래픽 처리
- **달성 전략**: 단계적 적용 (Phase 1 → 2 → 3)
- **기준**: 국내 커머스 피크 트래픽 수준

---

## Phase 1: 즉시 적용 (코드 변경 최소) — DONE

### REQ-1.1: Virtual Threads 활성화

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 배경 | Platform Thread 200개 제한으로 동시 처리량이 Tomcat thread pool에 의존 |
| 요구사항 | JDK 21 Virtual Threads를 활성화하여 blocking I/O 대기 시 스레드 풀 고갈 방지 |
| 수용 기준 | `spring.threads.virtual.enabled=true` 설정, 기존 테스트 전체 통과 |
| 제약사항 | `synchronized` 사용 금지 (`ReentrantLock` 사용), pinning 방지 |

### REQ-1.2: Tomcat 튜닝

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 배경 | 기본 `max-connections=8192`, `accept-count=100`은 대규모 트래픽에 부족 |
| 요구사항 | 동시 연결 수와 대기 큐를 확장하여 피크 트래픽 수용 |
| 수용 기준 | `max-threads=50`, `max-connections=10000`, `accept-count=200` |

### REQ-1.3: DB 커넥션 풀 확장

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 배경 | HikariCP `maximum-pool-size=40`은 Virtual Threads 환경에서 부족할 수 있음 |
| 요구사항 | 단일 DB 환경에서 커넥션 풀을 적절히 확장 |
| 수용 기준 | `maximum-pool-size=50` |
| 제약사항 | R/W 분리 전까지 과도한 확장 불필요 |

### REQ-1.4: 회원가입 중복체크 방식 전환

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 배경 | `findByLoginId` + `findByEmail` 2회 SELECT 후 중복 판단 → race condition 가능, 불필요한 DB 부하 |
| 요구사항 | DB Unique Constraint로 중복을 보장하고, 예외 처리로 응답 변환 |
| 수용 기준 | - `loginId`, `email` 컬럼에 `unique=true` 적용 |
|  | - 사전 조회 로직 제거 |
|  | - `DataIntegrityViolationException` → 409 CONFLICT 응답 변환 |
|  | - Facade 레이어에서 예외 catch (`@Transactional` 경계 밖) |
| 금지사항 | - `saveAndFlush()` 사용 금지 (Hibernate write-behind 최적화 파괴) |
|  | - Service 내부에서 `DataIntegrityViolationException` catch 금지 |

### REQ-1.5: 인증 헤더 통일 및 인증 처리 중앙화

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 배경 | 각 Controller에서 `@RequestHeader`로 인증 헤더를 직접 받아 중복 코드 발생 |
| 요구사항 | Interceptor + ArgumentResolver 패턴으로 인증을 중앙화 |
| 수용 기준 | - `@MemberAuthenticated` 어노테이션으로 인증 필요 API 표시 |
|  | - `AuthenticatedMember` 객체가 Controller 파라미터로 자동 주입 |
|  | - 인증 헤더: `X-Loopers-LoginId` / `X-Loopers-LoginPw` |
|  | - 인증 실패 시 401 UNAUTHORIZED 응답 |
|  | - Controller에서 인증 관련 코드 완전 제거 |
| 제약사항 | JWT 미사용 (의도적 결정) |

---

## Phase 2: 캐싱 및 인프라 확장 — 진행 중

### REQ-2.1: 인증 결과 캐싱 (BCrypt 호출 최소화)

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 배경 | 매 인증 요청마다 BCrypt 비교(~100ms) 수행 → 최대 ~2,000 TPS 병목. JWT 미사용 아키텍처에서 멀티 인스턴스 시 N×BCrypt 문제 |
| 요구사항 | Redis 글로벌 캐시로 인증 결과를 캐싱하여 BCrypt 호출 스킵 |
| 수용 기준 | - 캐시 키: `auth:{loginId}`, 값: `CachedAuth(memberId, loginId, passwordDigest)`, TTL 5분 |
|  | - SHA256으로 비밀번호 일치 확인 (BCrypt 대신) |
|  | - 캐시 히트 + 비밀번호 일치 시 `memberService.authenticate()` 호출 스킵 |
|  | - 캐시 히트 + 비밀번호 불일치 시 `authenticate()` 재호출 |
|  | - 비밀번호 변경 시 해당 `loginId`의 캐시 eviction |
|  | - AuthCacheStore(Application 포트) ← AuthCacheStoreImpl(Infrastructure 구현체) |
|  | - Master/Replica 분리, Redis 장애 시 DB fallback (try-catch) |
| 변경 이력 | 초기: Caffeine 로컬 캐시 (D5) → Redis 전환 (D41). 멀티 인스턴스 환경 대응 |
| Decision | D5, D41 |

### REQ-2.2: 회원 조회 캐싱

| 항목 | 내용 |
|------|------|
| 상태 | 보류 |
| 배경 | `getMember(id)` 조회가 빈번하게 발생, DB 부하 감소 필요 |
| 요구사항 | `member-cache`를 활용하여 회원 조회 결과 캐싱 |
| 수용 기준 | - `member-cache`: TTL 10분, max 10,000 엔트리 |
|  | - 회원 정보 변경 시 캐시 eviction |
| 비고 | 인증 캐시(auth-cache)가 BCrypt 병목을 해소하여 현재 긴급도 낮음. 실측 후 판단 |

### REQ-2.3: DB Read/Write 분리

| 항목 | 내용 |
|------|------|
| 상태 | 보류 |
| 배경 | 단일 DB에 읽기/쓰기가 집중 |
| 요구사항 | `AbstractRoutingDataSource` + MySQL Replica로 읽기 분산 |
| 보류 사유 | 현재 DB가 병목이 아님, 측정 후 판단 |
| 사전 준비 | `@Transactional(readOnly = true)` 이미 적용 완료 |

### REQ-2.4: Redis 상품 캐시 도입

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 배경 | 상품 조회가 가장 빈번한 API이며, 대규모 트래픽 환경에서 DB 부하 감소 필요 |
| 요구사항 | RedisTemplate 직접 사용 + Port 패턴으로 상품 상세/목록 캐시 |
| 수용 기준 | - 상품 상세: `product:detail:{id}` (TTL 5분, CUD 시 evict) |
|  | - 상품 목록: `product:list:{brand}:{sort}:{size}:{cursor}` (TTL 1분, LFU 자동 퇴출) |
|  | - ProductCacheStore(Application 포트) ← ProductCacheStoreImpl(Infrastructure 구현체) |
|  | - Master/Replica 분리 (읽기: Replica, 쓰기: Master) |
|  | - Redis 장애 시 DB fallback (try-catch, non-fatal) |
|  | - Redis maxmemory-policy: allkeys-lfu |
| Decision | D37 |

### REQ-2.5: Lettuce 커넥션 풀링

| 항목 | 내용 |
|------|------|
| 상태 | 보류 |
| 배경 | Redis 사용 시 단일 커넥션 멀티플렉싱의 한계 |
| 요구사항 | `LettucePoolingClientConfiguration` 적용 |
| 보류 사유 | Redis 활성 사용 중이나, 현재 트래픽 수준에서 단일 커넥션 멀티플렉싱으로 충분. 부하 테스트 후 판단 |

### REQ-2.6: 상품 인덱스 최적화

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 배경 | 10만건 환경에서 브랜드 필터 + 좋아요 순 정렬 시 filesort 발생 |
| 요구사항 | 복합 인덱스 추가로 filesort 제거 |
| 수용 기준 | - `idx_product_brand_status_like (brand_id, status, like_count DESC, id DESC)` |
|  | - `idx_product_brand_status_price (brand_id, status, price ASC, id DESC)` |
|  | - 기존 `idx_product_brand_id` 제거 (복합 인덱스에 포함) |
| Decision | D38 |

### REQ-2.7: like_count 배치 집계

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 배경 | D12에서 결정한 like_count 비정규화의 구체적 배치 구현 |
| 요구사항 | commerce-batch에서 Tasklet + JdbcTemplate으로 like_count 갱신 |
| 수용 기준 | - LikeCountSyncJobConfig + SyncLikeCountTasklet 구현 |
|  | - UPDATE JOIN 단일 쿼리로 전체 product.like_count 갱신 |
|  | - LikeCountSyncJobTest 통과 |
| Decision | D39 |

### REQ-2.8: 10만건 시드 데이터

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 배경 | 인덱스/캐시 성능 검증을 위한 대량 데이터 필요 |
| 요구사항 | ApplicationRunner + @Profile("local")로 앱 시작 시 자동 시드 |
| 수용 기준 | - 브랜드 20개, 상품 10만건, 좋아요 ~50만건 |
|  | - JdbcTemplate batchUpdate (1,000건 단위) |
|  | - 멱등: 이미 데이터 존재 시 스킵 |
| Decision | D40 |

### REQ-2.9: 브랜드 Redis 캐시 도입

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 배경 | 상품 조회 시 브랜드명을 항상 함께 반환. 변경 빈도 극히 낮은 데이터에 캐시 효과 극대화 |
| 요구사항 | RedisTemplate 직접 사용 + Port 패턴으로 브랜드 상세 캐시 |
| 수용 기준 | - 브랜드 상세: `brand:detail:{brandId}` (TTL 10분, CUD 시 evict) |
|  | - BrandCacheStore(Application 포트) ← BrandCacheStoreImpl(Infrastructure 구현체) |
|  | - ProductFacade에서 getCachedBrandName() 활용 |
|  | - AdminBrandFacade에서 수정/삭제 시 evict |
|  | - Master/Replica 분리, Redis 장애 시 DB fallback |
| Decision | D42 |

---

## Phase 3: 안정성 강화 — TODO

### REQ-3.1: Circuit Breaker

| 항목 | 내용 |
|------|------|
| 상태 | PARTIAL |
| 요구사항 | Resilience4j 기반 Circuit Breaker 적용 |
| 목적 | 외부 의존성 장애 시 빠른 실패 및 복구 |
| 진행 현황 | FEAT-9 결제 구현 중 PgClient에 @CircuitBreaker 적용 (D48). 전체 API 레벨 적용은 미착수 |

### REQ-3.2: Rate Limiting

| 항목 | 내용 |
|------|------|
| 상태 | TODO |
| 요구사항 | API 레벨 요청 제한 |
| 목적 | 악의적 트래픽 및 과부하 방지 |

### REQ-3.3: Graceful Degradation

| 항목 | 내용 |
|------|------|
| 상태 | TODO |
| 요구사항 | 폴백 패턴 적용 |
| 목적 | 부분 장애 시에도 핵심 기능 유지 |

---

## 2주차 설계 공통 결정사항

> 각 결정의 상세 배경·선택지·트레이드오프는 `DECISIONS.md`의 해당 번호를 참조한다.

| 결정 | 내용 | Decision |
|------|------|----------|
| 고객/어드민 Facade 분리 | 전체 도메인에 일괄 적용. Service는 공유, Facade만 분리. 확장성 확보 | D8 |
| 고객/어드민 DTO 분리 | Controller 레벨에서 노출 필드 분리. Facade는 동일 Info 반환 | D15 |
| 재고 검증 위치 | 도메인 모델 내부 (ProductModel.deductStock → 불변식 보호) | D9 |
| 본인 주문 검증 위치 | 도메인 모델 내부 (OrderModel.validateOwner → 비즈니스 규칙 보호) | D10 |
| Soft Delete 방식 | status=DELETED + deleted_at 병행. delete() 메서드에서 동시 설정 | D11 |
| 좋아요 수 집계 | product.like_count 컬럼 (DEFAULT 0) + commerce-batch 배치 갱신 (5분 주기) | D12 |
| 주문 번호 | UUID 방식 (내부 auto-increment ID 노출 방지) | D13 |
| INACTIVE/SUSPENDED 상태 | 미포함 (YAGNI. 현재 요구사항에 비활성화/판매중단 시나리오 없음) | D14 |
| SOLD_OUT 상태 | 제거. ProductStatus = ACTIVE/DELETED. stock=0이 품절 표현. soldOut Boolean 파생 필드로 고객 전달 | D16 |
| 브랜드명 중복 | 허용 (BR-B2 수정). UNIQUE 제거. soft delete 충돌 방지, 브랜드 식별은 PK 기반 | D19 |
| 좋아요/취소 멱등 | 양방향 멱등. Facade exists 사전 조회(200) + UNIQUE 안전망(race → 409). @Transactional Facade 주도 | D17, D33, D36 |
| Service 메서드 분리 | 고객용 (ACTIVE only) / 어드민용 (상태 무관) 분리. 확장성 확보 | D18 |
| DELETED 리소스 고객 접근 | 404 반환 (브랜드/상품 동일). 주문은 스냅샷으로 노출 | D21 |
| 좋아요 수 초기화 | product.like_count DEFAULT 0으로 상품 등록 시 자동 초기화 (별도 선삽입 불필요) | D20 |
| 좋아요 데이터 유지 | 브랜드/상품 삭제 시 product_like 유지. 내 목록에서 ACTIVE 필터링 | D22 |
| restoreStock / cancel | 미구현 (현재 스코프 외. 주문 취소 API 없음) | — |
| 물리 FK | 미사용. 논리적 참조만. 애플리케이션 레벨에서 참조 무결성 보장 | — |
| 어드민 주문 조회 | validateOwner 없음. 전체 주문 조회 가능 | D18 |
| 인증 전략 통일 | 대고객: @MemberAuthenticated 어노테이션 선택 적용. 어드민: @AdminAuthenticated 어노테이션 클래스 레벨 일괄 적용 → **3주차 D30에서 path 기반 Interceptor로 전환, 어노테이션 제거** | D3, D30 |
| VO 패턴 | `@JvmInline value class` + Entity primitive 저장. Service에서 `VO.of()` 생성. Hibernate 6.x AttributeConverter 미사용 | D23 |
| 스냅샷 범위 | productName, productPrice, brandName만 복사. quantity는 주문입력, amount는 파생값 | D25 |
| 주문 총액 비정규화 제거 | totalAmount 컬럼 미사용. `getTotalAmount() = orderItems.sumOf { it.amount }` 파생 계산 | D24 |
| 비밀번호 변경 시 캐시 eviction | MemberFacade에서 loginId 기반 Redis auth 캐시 evict | D5, D41 |
| 재고 동시성 제어 | 비관적 락(SELECT FOR UPDATE). Phase 1 기능 구현 시 포함 | D9 |
| 주문 플로우 동시성 최적화 | 쿠폰 차감(@Version + flush)을 재고 비관적 락 전에 실행. 불필요한 락 점유 방지 | D34 |
| 비관적/낙관적 락 비교 테스트 | 동일 시나리오(단일 자원 경합, 초과 경합)로 두 전략 동작 차이 검증 | D35 |
| 개발 순서 원칙 | Phase 1: 기능 정합성 → Phase 2: 동시성/멱등성/일관성/성능 | — |
| 상품 Redis 캐시 | RedisTemplate 직접 사용. ProductCacheStore 포트 패턴. 상세 5분/목록 1분 TTL. CUD evict | D37 |
| 복합 인덱스 최적화 | (brand_id, status, like_count DESC, id DESC) 등. filesort 제거 | D38 |
| like_count 배치 구현 | commerce-batch Tasklet + JdbcTemplate UPDATE JOIN. 단일 쿼리 갱신 | D39 |
| 시드 데이터 | ApplicationRunner + @Profile("local"). JdbcTemplate batchUpdate 10만건 | D40 |
| 인증 캐시 Caffeine→Redis 전환 | JWT 미사용 멀티 인스턴스 대응. AuthCacheStore 포트 패턴. N×BCrypt 문제 해소 | D41 |
| 브랜드 Redis 캐시 | BrandCacheStore 포트 패턴. TTL 10분. CUD evict. ProductFacade에서 활용 | D42 |

---

## 2주차 구현 체크리스트

> 구현 과제의 달성 여부를 추적한다. 모든 항목은 구현 완료 및 테스트 통과 후 체크한다.

### Product / Brand 도메인

- [x] 상품 정보 객체는 브랜드 정보(brandName), 좋아요 수(likeCount)를 포함한다
- [x] 상품의 정렬 조건(`latest`, `price_asc`, `likes_desc`)을 고려한 조회 기능을 설계했다
- [x] 상품은 재고를 가지고 있고, 주문 시 차감할 수 있어야 한다
- [x] 재고의 음수 방지 처리는 도메인 레벨에서 처리된다 (`ProductModel.deductStock()`)

### Like 도메인

- [x] 좋아요는 유저와 상품 간의 관계로 별도 도메인으로 분리했다 (`domain/like/`)
- [x] 상품의 좋아요 수는 상품 상세/목록 조회에서 함께 제공된다 (`product.like_count`)
- [x] 단위 테스트에서 좋아요 등록/취소 흐름을 검증했다 (`LikeServiceIntegrationTest`, `LikeV1ApiE2ETest`)

### Order 도메인

- [x] 주문은 여러 상품을 포함할 수 있으며, 각 상품의 수량을 명시한다 (`OrderItem.quantity`)
- [x] 주문 시 상품의 재고 차감을 수행한다 (`OrderFacade → ProductService.deductStock`)
- [x] 재고 부족 예외 흐름을 고려해 설계되었다 (`ProductModel.deductStock` → `CoreException(BAD_REQUEST)`)
- [x] 단위 테스트에서 정상 주문 / 예외 주문 흐름을 모두 검증했다 (`OrderModelTest`, `OrderServiceIntegrationTest`, `OrderV1ApiE2ETest`)

### 도메인 서비스

- [x] 도메인 내부 규칙은 Domain Service / Domain Model에 위치시켰다 (`deductStock`, `validateOwner`, `delete`)
- [x] 상품 상세 조회 시 Product + Brand 정보 조합은 Application Layer에서 처리했다 (`ProductFacade → ProductService + BrandService`)
- [x] 복합 유스케이스는 Application Layer에 존재하고, 도메인 로직은 위임되었다 (`OrderFacade`의 재고 차감 + 주문 생성 조합)
- [x] 도메인 서비스는 상태 없이(`@Component` stateless), 동일한 도메인 경계 내의 도메인 객체의 협력 중심으로 설계되었다

### 소프트웨어 아키텍처 & 설계

- [x] 전체 프로젝트의 구성은 DIP 기반이다: `Application → Domain ← Infrastructure`
- [x] Application Layer(Facade)는 도메인 객체를 조합해 흐름을 orchestration 했다
- [x] 핵심 비즈니스 로직은 Entity(`deductStock`, `validateOwner`), VO(`LoginId.of`, `BrandName.of`), Domain Service에 위치한다
- [x] Repository Interface는 Domain Layer에 정의되고(`domain/{domain}/`), 구현체는 Infrastructure에 위치한다(`infrastructure/{domain}/`)
- [x] 패키지는 계층 + 도메인 기준으로 구성되었다 (`/domain/order`, `/application/like`, `/interfaces/api/brand` 등)
- [x] 테스트는 외부 의존성을 분리하고, TestContainers(MySQL)를 사용해 통합 테스트가 가능하게 구성되었다. E2E 테스트에서 실제 API 호출을 검증한다

---

## 3주차: 아키텍처 리팩토링 — DONE

### REQ-A1: DIP 전 도메인 적용 (Domain Model / JpaModel 분리)

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 배경 | Domain Model이 JPA Entity를 겸하여 @Entity, JPA 어노테이션이 Domain 레이어에 노출. 모듈 분리 시 순환 의존 발생 |
| 요구사항 | Domain Model(data class)과 Infrastructure JpaModel(@Entity)을 분리하여 DIP 완전 적용 |
| 수용 기준 | - 6개 도메인(Member, Brand, Product, Order, Like, Example) 전체 적용 |
|  | - DIP 위반 0건 (Presentation→Domain 0, Presentation→Infrastructure 0, Application→Infrastructure 0) |
|  | - JpaModel에 toModel()/from()/updateFrom() 변환 메서드 구현 |
|  | - 전체 테스트 통과, ktlint 클린 |
| 트레이드오프 | JpaModel ↔ Model 변환 보일러플레이트 증가 |
| Decision | D30 |

### REQ-A2: QueryDSL 전환

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 배경 | 상품 커서 페이징에서 StringBuilder 기반 JPQL 사용 → 타입 안전하지 않음 |
| 요구사항 | QueryDSL 타입 안전 API로 동적 커서 쿼리 전환. JPA/QueryDSL Repository 분리 |
| 수용 기준 | - ProductQueryDslRepository 분리 (커서 페이징 전담) |
|  | - BooleanExpression 기반 동적 커서 조건 (null 자동 무시) |
|  | - ProductRepositoryImpl은 JPA/QueryDSL 양쪽에 위임만 수행 |
|  | - 기존 테스트 전체 통과 |

### REQ-A3: 타입 안전 정렬 (ProductSortRequest enum)

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 배경 | Controller에서 sort 파라미터를 String으로 수신 → Facade에서 문자열 변환 필요 |
| 요구사항 | Presentation 레이어에 ProductSortRequest enum 도입, 컴파일 타임 타입 안전성 확보 |
| 수용 기준 | - ProductSortRequest enum (Presentation) → toDomain() → ProductSort (Domain) |
|  | - StringToProductSortRequestConverter로 case-insensitive 바인딩 (?sort=latest 호환) |
|  | - Facade는 ProductSort 직접 수신 (String 변환 로직 제거) |

### REQ-A4: 인증 구조 단순화

| 항목 | 내용 |
|------|------|
| 상태 | DONE |
| 배경 | @MemberAuthenticated/@AdminAuthenticated 어노테이션 + Interceptor가 MemberService/CacheManager 직접 참조 → DIP 위반 |
| 요구사항 | 어노테이션 기반 → Interceptor path 패턴 기반 인증 전환. AuthService(Application) 도입 |
| 수용 기준 | - @MemberAuthenticated/@AdminAuthenticated 어노테이션 제거 |
|  | - Interceptor가 URL path 패턴으로 인증 대상 판별 |
|  | - AuthService(Application)에서 인증 + Redis 캐시 통합 관리 (초기 Caffeine → D41에서 Redis 전환) |
|  | - Interceptor → AuthService 단일 의존 (Domain/Infrastructure 직접 참조 제거) |
| Decision | D30 |

---

## 3주차 구현 체크리스트

> DIP 아키텍처 리팩토링 달성 여부를 추적한다.

### DIP 분리

- [x] 6개 도메인 Domain Model(data class) + Infrastructure JpaModel(@Entity) 분리 완료
- [x] DIP 위반 0건 달성 (grep 기반 검증)
- [x] Domain 레이어에 JPA/Spring/Infrastructure 의존 없음
- [x] JpaModel에 toModel()/from()/updateFrom() 변환 표준화

### 예외 계층

- [x] CoreException/ErrorType → Domain 레이어로 이동
- [x] DomainExceptionTranslator(@Aspect) → ApplicationException 변환
- [x] Presentation은 ApplicationException만 처리

### 인증 구조

- [x] @MemberAuthenticated/@AdminAuthenticated 어노테이션 제거
- [x] AuthService(Application) 도입 — 인증 + 캐시 통합
- [x] Interceptor → AuthService 단일 의존

### QueryDSL & 타입 안전

- [x] ProductQueryDslRepository 분리 (JPA/QueryDSL 책임 분리)
- [x] StringBuilder JPQL → QueryDSL BooleanExpression 동적 쿼리
- [x] ProductSortRequest enum 도입 (Presentation → Domain 변환)
- [x] StringToProductSortRequestConverter 등록 (case-insensitive)

### 포트 패턴

- [x] PasswordEncryptor 인터페이스(Domain) ← BcryptPasswordEncryptor 구현(Infrastructure)
- [x] Repository 인터페이스(Domain) ← RepositoryImpl 구현(Infrastructure)

### 검증

- [x] 198개 테스트 전체 통과
- [x] ktlint 클린
- [x] DIP 위반 0건 (Presentation→Domain, Presentation→Infrastructure, Application→Infrastructure)

---

## 공통 제약사항

| 제약 | 설명 |
|------|------|
| JWT 미사용 | 의도적 결정. 모든 인증은 `X-Loopers-LoginId` / `X-Loopers-LoginPw` 헤더 기반 |
| BCrypt 반복 호출 금지 | 매 요청 CPU-intensive 연산 반복 금지, 캐싱 레이어로 해결 |
| 사전 조회 중복체크 금지 | DB Unique Constraint + 예외 처리 방식 사용. 단, 멱등 200 반환 목적(좋아요 등)의 사전 조회는 허용 (D36) |
| `synchronized` 금지 | Virtual Thread pinning 방지, `ReentrantLock` 사용 |
| `saveAndFlush()` 금지 | Hibernate write-behind 최적화 파괴 방지. 단, @Version 낙관적 락의 조기 충돌 감지 목적 flush()는 예외 (D34) |
| null-safety 필수 | Kotlin null-safety 활용 |
| TDD 워크플로우 | Red → Green → Refactor, 3A 원칙 (Arrange-Act-Assert) |
| 도메인 확장 패턴 | example 패키지 구조 참고, 4-layer 구조 유지 |

---

## 진행 현황 요약

### 기능 요구사항

| 도메인 | 전체 | 완료 | 구현중 | 미착수 |
|--------|------|------|--------|--------|
| 회원 관리 | 3 | 3 | 0 | 0 |
| 브랜드 | 1 | 1 | 0 | 0 |
| 상품 | 1 | 1 | 0 | 0 |
| 좋아요 | 1 | 1 | 0 | 0 |
| 주문 | 1 | 1 | 0 | 0 |
| 쿠폰 | 1 | 1 | 0 | 0 |
| 결제 | 1 | 1 | 0 | 0 |
| 선착순 쿠폰 | 1 | 1 | 0 | 0 |
| 랭킹/추천 | 1 | 1 | 0 | 0 |
| **합계** | **11** | **11** | **0** | **0** |

### 아키텍처 요구사항

| 항목 | 전체 | 완료 | 미착수 |
|------|------|------|--------|
| 3주차 리팩토링 (REQ-A1~A4) | 4 | 4 | 0 |
| Event-Command-Handler (ARCH-1) | 1 | 1 | 0 |
| Transactional Outbox Pattern (ARCH-2) | 1 | 1 | 0 |
| 유저 행동 로깅 AOP (ARCH-3) | 1 | 1 | 0 |

### 성능 요구사항

| Phase | 전체 | 완료 | 보류 | 미착수 |
|-------|------|------|------|--------|
| Phase 1 | 5 | 5 | 0 | 0 |
| Phase 2 | 9 | 6 | 3 | 0 |
| Phase 3 | 3 | 0 | 0 | 3 |
| **합계** | **17** | **11** | **3** | **3** |
