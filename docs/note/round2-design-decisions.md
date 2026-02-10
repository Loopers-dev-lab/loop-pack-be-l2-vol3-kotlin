# Round 2 설계 결정 기록

이커머스 도메인(브랜드, 상품, 좋아요, 주문) 설계 과정에서 논의하고 결정한 사항들을 정리한다.

---

## 1. 엔티티 설계

### 1.1 Like 엔티티 — BaseEntity 미상속
- **결정:** Like는 BaseEntity를 상속하지 않는다. id, userId, productId만 보유
- **이유:** 이력 관리가 불필요하고, 취소 시 하드 딜리트(물리 삭제)로 관리
- **id 생성:** `@GeneratedValue(strategy = GenerationType.IDENTITY)` 직접 선언
- **likes 테이블에 created_at 없음:** 최신순 정렬이 필요하면 id 역순으로 대체 (YAGNI)

### 1.2 OrderItem — Aggregate 내부 Entity (종속 엔티티)
- **결정:** OrderItem은 독립 Entity가 아닌 Order Aggregate의 내부 Entity
- **특성:** id를 가지지만 독립 생명주기와 독립 Repository가 없다. 항상 Order를 통해서만 접근
- **OrderItem에서 orderId 제거:** 단방향 `@OneToMany(cascade = ALL, orphanRemoval = true)` + `@JoinColumn(name = "order_id", nullable = false)`로 구현. OrderItem에서 Order로의 역참조(`@ManyToOne`) 없음
- **nullable = false 의미:** Hibernate의 extra UPDATE 문제를 방지. INSERT 시 order_id를 함께 설정

### 1.3 enum inner class 배치
- **결정:** ProductStatus는 Product의 inner enum, OrderStatus는 Order의 inner enum
- **이유:** 각 도메인 내부에서만 사용되므로 별도 파일 분리 불필요

---

## 2. 주문 도메인

### 2.1 Order.totalPrice 반정규화
- **결정:** 주문 생성 시 totalPrice를 계산하여 Order 엔티티에 저장
- **이유:** 주문 목록 조회 시 OrderItem을 로딩하지 않고도 총액 제공
- **계산 시점:** `Order.create()` 정적 팩토리 메서드에서 items의 `productPrice * quantity` 합산

### 2.2 Order.create() 정적 팩토리
- **결정:** `Order.create(userId, items)` 정적 팩토리에서 Order + OrderItem을 한 번에 생성
- **책임:** OrderItem 생성 + totalPrice 계산 + 중복 상품 병합

### 2.3 중복 상품 병합
- **결정:** 같은 productId가 여러 항목에 포함되면 수량을 합산하여 하나의 OrderItem으로 병합
- **위치:** OrderService.createOrder 내부에서 도메인 로직으로 처리

### 2.4 주문 목록 조회 기간 필터
- **파라미터:** startedAt/endedAt (LocalDateTime)
- **범위:** startedAt >= (inclusive), endedAt < (exclusive) — half-open 범위
- **기준:** createdAt
- **Repository 메서드:** `findOrdersByUserIdInPeriod` (Between은 양쪽 inclusive를 암시하므로 커스텀 네이밍)
- **TimeZone:** CommerceApiApplication의 DefaultTimeZone으로 ZonedDateTime 변환. 명시적 TimeZone 전달 시 해당 것 사용

### 2.5 OrderService 메서드 정리
- **getOrder(orderId):** 범용 — 고객(Facade에서 userId 확인) + 어드민(확인 없음) 모두 사용
- **getOrdersByUserId(...):** 고객 주문 목록 (SQL WHERE userId 필요)
- **getAllOrders(pageable):** 어드민 주문 목록
- **getOrderByUserIdAndId 제거:** getOrder + Facade userId 확인으로 대체

---

## 3. 좋아요 도메인

### 3.1 삭제된 상품에 대한 좋아요 처리
- **등록:** 404 (삭제된 상품에 좋아요 등록 불가)
- **취소:** 200 (Like 레코드 있으면 삭제, 없으면 무시. 멱등). likeCount는 갱신하지 않음 (상품이 삭제 상태)
- **목록 조회:** 삭제된 상품은 미노출
- **잔존 데이터:** 삭제된 상품의 Like 레코드는 배치로 정리

---

## 4. 인증/인가

### 4.1 @AuthUser + AuthUserArgumentResolver
- **결정:** `@AuthUser` 커스텀 어노테이션 + `AuthUserArgumentResolver` (HandlerMethodArgumentResolver 구현체)
- **흐름:** AuthInterceptor가 request.setAttribute("userId", user.id) → AuthUserArgumentResolver가 `@AuthUser userId: Long` 파라미터에 주입
- **용도:** 좋아요, 주문 등 인증이 필요한 API에서 Controller 메서드 파라미터로 userId 주입

---

## 5. 아키텍처 결정

### 5.1 Facade 유지 (1:1 passthrough 포함)
- **결정:** 단일 도메인 1:1 매핑인 경우에도 Facade를 유지
- **이유:**
  - Controller는 항상 Facade만 의존 → 의존성이 단순하고 일관
  - cross-domain 로직 추가 시 Controller 변경 없이 Facade만 확장
  - Info/Dto 변환이 항상 application 레이어에서 일어남
- **단, 1:1 Facade 메서드에 @Transactional은 붙이지 않음** — Service에 위임

### 5.2 @Transactional 전략
- **Service:** 변경 작업에 `@Transactional` 필수 적용
- **Facade:** 선택적 적용 — 여러 Service를 조합하여 원자성이 필요한 경우에만
  - 정당한 케이스: 좋아요 등록/취소 (LikeService + ProductService), 브랜드 삭제 cascade (BrandService + ProductService), 상품 등록 (BrandService + ProductService), 주문 생성 (ProductService + OrderService)
- **단일 Service 호출 Facade:** Service의 @Transactional에 위임 (Facade에 @Transactional 안 붙임)

### 5.3 soft delete 조회 전략
- **결정:** `@Where(clause = "deleted_at IS NULL")` 미사용
- **대신:** Repository 메서드마다 `deletedAt IS NULL` 조건을 명시적으로 추가
- **이유:** 어드민 API에서 삭제된 데이터도 조회해야 하므로 @Where는 유연성을 제한

---

## 6. 인덱스 전략

### 6.1 products 복합 인덱스
- **결정:** `(deleted_at, status, like_count DESC)` 복합 인덱스 사용
- **이유:** 대고객 상품 목록 쿼리(`WHERE deleted_at IS NULL AND status != 'HIDDEN' ORDER BY like_count DESC`)에서 MySQL은 하나의 인덱스만 사용하므로, WHERE + ORDER BY를 함께 커버하는 복합 인덱스가 효율적
- **기존 `(status, deleted_at)` + `(like_count)` 별도 인덱스를 통합**

### 6.2 likes UNIQUE 인덱스
- `(user_id, product_id)` UNIQUE — 중복 좋아요 방지 + user_id 단독 조회도 커버

---

## 7. 향후 검토 사항

### 7.1 재고 동시성
- **현재:** 단일 요청 기준으로 처리 (향후 과제)
- **후보 방안:**
  - Atomic SQL UPDATE: `UPDATE products SET stock = stock - :qty WHERE id = :id AND stock >= :qty` (가장 단순)
  - @Version (optimistic lock): 충돌 시 재시도 로직 필요
  - SELECT FOR UPDATE: 비관적 락, 대기 시간 발생
  - Redis Lua: 최고 성능, Redis 도입 필요

### 7.2 JPA 단방향 @OneToMany 주의점
- `@JoinColumn(nullable = false)` 명시로 extra UPDATE 방지
- Hibernate 6.x (Spring Boot 3.4)에서 INSERT 시 order_id를 함께 설정

### 7.3 Like 멱등 처리 동시성
- check-then-act 패턴에서 동시 요청 시 UNIQUE 제약 위반 가능
- DataIntegrityViolationException catch + 멱등 처리, 또는 INSERT IGNORE 패턴 검토
