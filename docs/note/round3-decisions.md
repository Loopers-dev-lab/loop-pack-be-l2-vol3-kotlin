# Round 3 설계 결정 기록

Round 3에서 도입된 도메인 모델링, 바운디드 컨텍스트, 단일 서비스 전략에 대한 설계 결정과 근거를 기록한다.

---

## 중요도 분류

|  중요도   | 섹션                                                                         | 핵심 질문                                        |
|:------:|----------------------------------------------------------------------------|----------------------------------------------|
| **최상** | [1. Repository 인터페이스 위치와 도메인 순수성](#1-repository-인터페이스-위치와-도메인-순수성)         | DIP는 누구를 보호하는가?                              |
| **최상** | [2. Pragmatic Clean Architecture](#2-pragmatic-clean-architecture)         | Domain Model과 JPA Entity를 왜, 어떻게 분리하는가?      |
| **상**  | [3. Catalog 바운디드 컨텍스트](#3-catalog-바운디드-컨텍스트)                               | 도메인 경계를 어디에 그을 것인가?                          |
| **상**  | [4. Catalog BC — Domain Service 없이 UseCase 직접 호출](#4-catalog-bc--domain-service-없이-usecase-직접-호출) | 같은 BC 내 서비스를 왜 쪼개지 않는가?                      |
| **상**  | [5. Application 계층 — UseCase](#5-application-계층--usecase)                  | 트랜잭션 경계와 오케스트레이션 책임은 어디에?                    |
| **상**  | [6. 계약에 의한 설계 (DbC)](#6-계약에-의한-설계-dbc)                                     | 검증은 어느 레이어에?                                 |
| **상**  | [7. 어드민/대고객 서비스 메서드 분리](#7-어드민대고객-서비스-메서드-분리)                              | 대고객 조회를 어드민 CUD에서 재사용해도 되는가?                 |
| **중**  | [8. 기타 설계 결정](#8-기타-설계-결정)                                                 | Point 서비스 분리, Order 생성 패턴, 조회 메서드 분리, 도메인 경계 |
| **중**  | [9. 동시성/멱등성](#9-동시성멱등성)                                                    | 포인트 동시성, 멱등성, 재고 전략, 2-Phase 주문              |
| **참고** | [10. Round 2와의 차이 요약](#10-round-2와의-차이-요약)                                 | 변경 이력                                        |

---

## 1. Repository 인터페이스 위치와 도메인 순수성

### 1.1 Domain 계층에 둔다 (Blue Book 스타일)

**결정:** Repository 인터페이스는 Domain 계층에 둔다. Domain Service가 Repository를 직접 호출한다.

**근거:**

- DIP는 보호하고 싶은 계층이 인터페이스를 소유해야 의미가 있다. 보호 대상은 Domain이다
- "UserRepository.findByLoginId"는 도메인 언어 — 영속화 계약 자체가 도메인의 일부다
- Domain Service가 Repository를 사용할 수 없으면, 모든 영속화 호출이 Application으로 올라가고 Domain이 빈약해진다

코치 피드백: "도메인 데이터를 영속화하는 것 자체가 도메인 비즈니스."

### 1.2 Spring Data 타입 제거 (Pageable/Page)

**결정:** Domain Repository에서 `Pageable`/`Page` 제거. 도메인 고유 타입 `PageResult<T>`를 도입한다.

**레이어별 책임:**

- Controller: `page: Int, size: Int` 수신 + 검증, `PageResult` → Spring `Page<T>`로 변환
- Domain Service/Repository: `page: Int, size: Int` → `PageResult<T>` (Spring 의존 없음)
- Infrastructure 구현체: `PageRequest.of()` → JPA 호출 → `PageResult` 변환

---

## 2. Pragmatic Clean Architecture

### 2.1 Domain Model ↔ JPA Entity 분리

**결정:** Domain Model(순수 POJO)과 JPA Entity(DB 매핑 전용)를 완전 분리한다.

| 구분    | Domain Model (`domain/`)        | JPA Entity (`infrastructure/`) |
|-------|---------------------------------|--------------------------------|
| 역할    | 비즈니스 규칙, 상태 변경/판단               | DB 테이블 매핑 전용                   |
| 애노테이션 | 없음 (순수 POJO)                    | `@Entity`, `@Column` 등         |
| 검증    | `init { validate() }`, VO 자가 검증 | 없음 (Domain Model에 위임)          |
| 매핑    | —                               | `fromDomain()` / `toDomain()`  |

Round 2의 `guard()` (`@PrePersist`/`@PreUpdate`) 패턴을 `init { validate() }` + VO 자가 검증으로 대체하였다.

### 2.2 Value Object 적극 활용

Domain Model이 순수 POJO이므로 `@Converter` 부담 없이 VO를 자유롭게 사용한다:

- 단일 값: `@JvmInline value class` (Price, Email, BrandName 등)
- 도메인 메서드 포함: 일반 `class` (Stock, Point)
- 단, JPA Entity 계층에서는 @Converter 보일러플레이트를 방지하기 위해 원시 타입(Long)을 유지한다.

### 2.3 JPA 연관관계 미사용

`@OneToMany`, `@ManyToOne` 등을 사용하지 않는다. 모든 연관 데이터는 Repository를 통해 명시적으로 조회하여 N+1 문제를 원천 차단한다.

### 2.4 Repository 구조

`XxxRepositoryImpl.kt` 하나에 `internal interface XxxJpaRepository`와 구현체를 함께 선언한다. JpaRepository는 `internal`로 외부 노출을 차단한다.

### 2.5 트레이드오프

- **얻은 것:** Domain Model 순수성, VO 자유도, 단위 테스트 용이성, JPA 관심사 격리
- **감수:** `fromDomain()`/`toDomain()` 매핑 보일러플레이트, 스키마 변경 시 양쪽 수정 필요

---

## 3. Catalog 바운디드 컨텍스트

### 3.1 Product + Brand를 Catalog로 통합

**결정:** Product와 Brand를 하나의 Catalog 바운디드 컨텍스트로 통합한다.

**근거:**

- 상품 상세 조회, 상품 등록, 브랜드 삭제 — 모두 Product + Brand를 함께 다루는 단순 조합이다
- 별도 도메인으로 두면 매번 cross-domain 오케스트레이션이 필요하다
- 통합으로 UseCase가 같은 경계 내의 Repository를 직접 호출하면 된다

멘토 조언: "Catalog라는 바운디드 컨텍스트의 서브도메인으로 생각하면, Facade를 거칠 필요 없이 직접 호출할 수 있다."

> 과제 체크리스트에 "Product + Brand 조합은 Application Layer에서 처리"라고 되어 있으나, Catalog BC를 도입하면 같은 경계 내이므로 UseCase가 두 Repository를
> 직접 호출하여 처리하는 것이 자연스럽다.

### 3.2 Like는 별도 도메인 유지

Like는 "사용자의 관심 표현"이지 카탈로그 정보가 아니다. 향후 추천/랭킹 확장 가능성, 변경 이유가 다른 개념 혼재 방지를 위해 분리 유지한다.

### 3.3 Brand는 탐색/전시용

현재 Brand는 name만 가진 단순 분류 정보다. 정산/계약 속성이 없으므로 Catalog 내부에 포함한다. 향후 Partner/MD 컨텍스트가 생기면 분리 검토.

---

## 4. Catalog BC — Domain Service 없이 UseCase 직접 호출

**결정:** Catalog BC에 별도 Domain Service(CatalogService 등)를 두지 않는다. UseCase가 ProductRepository + BrandRepository를 직접 주입받아 처리한다.

**근거:**

- Product + Brand 조합은 원자적 얽힘이 아닌 단순 조합이므로 Domain Service가 불필요하다
- Domain Service를 두면 빈 껍데기 프록시가 되어 UseCase → Service → Repository 불필요한 간접 호출이 생긴다
- 복잡도는 서비스가 아닌 도메인 컴포넌트(Entity, VO)로 흡수한다
- 비대화 시 대응: UseCase를 쪼개는 것이 아니라 경계를 재조정한다

멘토 피드백: "최상위 서비스는 하나만 만들고, 그 안에서 복잡도는 도메인 컴포넌트로 풀어내는 걸 선호합니다."

---

## 5. Application 계층 — UseCase

### 5.1 구조

Application 계층은 UseCase로만 구성된다. 단일 도메인이든 cross-domain이든 UseCase가 직접 필요한 Repository와 Domain Service를 주입받아 오케스트레이션한다.

- **기본 패턴**: UseCase가 Repository를 직접 호출. 의미 없는 빈 껍데기 Domain Service를 만들지 않는다
- **Domain Service 사용 시점**: 여러 Entity 간 원자적 얽힘이 있는 경우에만 (예: `PointDeductor` — 잔고 차감 + 이력 생성)
- **cross-domain 오케스트레이션**: UseCase가 여러 도메인의 Repository를 직접 주입받아 처리 (예: `PlaceOrderUseCase`가 ProductRepository + OrderRepository + PointDeductor 조합)

### 5.2 @Transactional 위치

**결정:** `@Transactional`은 UseCase에 둔다. Domain Service에는 두지 않는다.

**근거:** Domain Service는 순수 도메인 로직 조율에 집중한다. 트랜잭션 경계를 UseCase에 두면 "어느 작업이 하나의 트랜잭션 단위인가"를 Application 계층에서 일관되게 파악할 수 있다.

### 5.3 AuthService 제거

AuthInterceptor → `AuthenticateUserUseCase` → UserRepository 흐름. 인증도 UseCase를 거치는 Strict Layered Architecture를 따른다.

### 5.4 UseCase 네이밍 규칙

| 대상  | 패턴                | 예시                       |
|-----|-------------------|--------------------------|
| 대고객 | `XxxUseCase`      | `GetProductUseCase`      |
| 어드민 | `XxxAdminUseCase` | `GetProductAdminUseCase` |

---

## 6. 계약에 의한 설계 (DbC)

### 6.1 원칙

- **불변식은 Domain Model/VO 내부에 선언** — `init` 블록, VO 자가 검증
- **사전 조건은 가능한 한 Domain Model에 둔다** — Service에만 있으면 우회 가능
- **도메인 판단 로직은 Domain Model이 소유** — Service가 내부 상태를 직접 꺼내 판단하지 않는다

**보충:** Service의 early-return 검증은 허용한다. DB 통신 전에 동일한 사전 조건을 먼저 검증하여 불필요한 호출을 방지. 에러 메시지는 Entity와 통일.

### 6.2 검증 레이어 분담

| 레이어            | 검증 대상         | 메커니즘                             |
|----------------|---------------|----------------------------------|
| Controller     | 입력 형식, 범위     | Bean Validation (`@Min`, `@Max`) |
| Domain Model   | 불변식, 사전/사후 조건 | `init` 블록, 도메인 메서드 내 검증          |
| VO             | 값의 도메인 규칙     | `init` 블록 자가 검증                  |
| Domain Service | 존재 여부, 도메인 정책 | Repository 조회 + Entity 메서드 위임    |

### 6.3 주요 도메인 객체 계약

**Product:**

- 불변식: `price >= 0` (Price VO), `stock >= 0` (Stock VO), `likeCount >= 0`
- `isActive()`: `deletedAt == null && status != HIDDEN`
- `isAvailableForOrder()`: `deletedAt == null && status == ON_SALE`

**UserPoint:**

- 불변식: `balance >= 0` (Point VO)
- `charge(amount)`: amount > 0, balance + amount ≤ MAX_BALANCE
- `use(amount)`: amount > 0, balance ≥ amount

**PointHistory:**

- 불변식: `amount > 0`

---

## 7. 어드민/대고객 서비스 메서드 분리

### 7.1 원칙

대고객용 조회 메서드(`getActive*`)는 어드민 CUD 경로에서 재사용하지 않는다.

| 컨텍스트    | 조회 메서드                                   | 반환 범위    |
|---------|------------------------------------------|----------|
| 대고객 조회  | `getActiveBrand()`, `getActiveProduct()` | 활성 데이터만  |
| 어드민 조회  | `getBrand()`, `getProduct()`             | 모든 상태 포함 |
| 어드민 CUD | `getBrand()`, `getProduct()`             | 모든 상태 포함 |

### 7.2 Repository deletedAt 필터링

- `findById` — `deletedAt IS NULL` 필터링 포함 (기본)
- `findByIdIncludeDeleted` — 삭제된 엔티티 포함 (Restore UseCase 전용)

### 7.3 Soft Delete 복구 정책

- 브랜드 삭제 시 소속 상품 cascade soft delete
- 복구 시 cascade 복구하지 않음 (삭제 시점에 개별 삭제와 cascade 삭제를 구분할 수 없으므로)

### 7.4 권한 경계 요약

| 대상      | 대고객                              | 어드민         |
|---------|----------------------------------|-------------|
| Brand   | 활성만 조회                           | 전체 조회/수정/복구 |
| Product | 활성만 조회 (HIDDEN/삭제 → 404)         | 전체 조회/수정/복구 |
| Order   | 본인 주문만 (타인 → 404)                | 전체 조회       |
| Like    | 삭제/HIDDEN 상품 좋아요 불가, 목록은 조용한 필터링 | —           |

---

## 8. 기타 설계 결정

### 8.1 회원가입 cross-domain 오케스트레이션

회원가입 시 User 생성 + UserPoint 초기화는 cross-domain 작업이다. `RegisterUserUseCase`가 UserRepository + UserPointRepository를 직접 주입받아 하나의 트랜잭션으로 처리한다.

### 8.2 Point Domain Service 분리 — PointDeductor + PointCharger

포인트 잔액 변경 + 이력 생성은 원자적 얽힘이 있으므로 Domain Service로 분리한다. 충전(`PointCharger`)과 차감(`PointDeductor`)을 분리한 이유: 충전은 향후 결제 UseCase의 조율 대상이 될 서비스다. 합치면 UseCase가 서비스 일부 메서드만 호출하는 어색한 구조가 된다.

### 8.3 Order 생성 패턴 — private constructor + create()

Product/UserPoint(`public constructor + init { validate() }`)와 달리 Order는 `private constructor + create()` 패턴. 생성 자체가 복합
조립(OrderItem + totalPrice 계산)이므로 팩토리 메서드가 유일한 생성 경로여야 한다.

**OrderProductInfo 도입:** `Order.create()`가 `Product` 타입을 직접 받으면 cross-domain 의존이 생긴다. `OrderProductInfo(id, name, price)` 데이터 클래스를 도입하여, Product → OrderProductInfo 변환은 `PlaceOrderUseCase`에서 수행.

### 8.4 조회 메서드 분리 — 호출 컨텍스트에 따른 검증 수준

| 메서드                      | 호출처                  | 전략                          |
|--------------------------|----------------------|-----------------------------|
| `getActiveProductsByIds` | `GetUserLikesUseCase` | 관대한 필터링 — 삭제/HIDDEN 조용히 걸러냄 |
| `getProductsForOrder`    | `PlaceOrderUseCase`  | 엄격한 검증 — 문제 있으면 즉시 예외       |

### 8.5 도메인 경계 정의

| BC          | Domain Model/VO                                      | Domain Service                  |
|-------------|------------------------------------------------------|---------------------------------|
| **User**    | User                                                 | — (UseCase 직접 호출)              |
| **Catalog** | Product, Brand, Stock(VO), ProductStatus             | — (UseCase 직접 호출)              |
| **Like**    | Like                                                 | — (UseCase 직접 호출)              |
| **Order**   | Order, OrderItem, OrderProductInfo, OrderStatus      | — (UseCase 직접 호출)              |
| **Point**   | UserPoint, PointHistory, Point(VO), PointHistoryType | PointDeductor, PointCharger    |

### 8.6 findItemsByOrders → 확장 함수

`GetOrdersUseCase`와 `GetOrdersAdminUseCase`에 중복되어 있던 `findItemsByOrders()`를 `OrderItemRepository` 확장 함수로 추출하였다.

### 8.7 CatalogCommand 원시 타입 전환

Command의 필드를 VO 타입에서 원시 타입으로 변경. VO 생성(검증)은 UseCase 내부에서 수행한다. Command는 Application 계층 객체이므로 도메인 VO에 의존하지 않는다.

---

## 9. 동시성/멱등성

### 9.1 포인트 동시성 (해결)

`findByUserIdForUpdate()`에 JPA `@Lock(PESSIMISTIC_WRITE)` 비관적 락 적용. 포인트는 user:userPoint = 1:1 관계(단일 row), 낮은 컨텐션이므로 비관적
락이 적합.

### 9.2 usePoints/chargePoints 멱등성 (보류)

동일 요청 중복 처리를 방지하는 멱등성 키가 없다. 결제 도메인 도입 시 `idempotencyKey` 전파 경로와 함께 설계 예정.

### 9.3 재고 동시성 — Redis 전략 (보류)

재고는 높은 트래픽 + 높은 정합성이 모두 요구된다. Redis Lua script로 atomic decrement 후 DB 비동기 반영 방향. 인프라 변경 범위가 크므로 향후 라운드에서 구현.

### 9.4 2-Phase 주문 (보류)

현재 단일 `@Transactional`에서 재고 차감 → 주문 생성 → 포인트 차감을 수행. 선착순 재고 경쟁 후 포인트 부족으로 실패 시 재고까지 롤백되는 문제. PENDING → CONFIRMED 상태 기반
2-Phase 처리로 개선 가능하나, 변경 범위가 크므로 보류.

---

## 10. Round 2와의 차이 요약

| 항목             | Round 2                                             | Round 3                                         |
|----------------|-----------------------------------------------------|-------------------------------------------------|
| 도메인 경계         | Product, Brand 별도                                   | Catalog (Product + Brand 통합)                    |
| 서비스 구조         | ProductService + BrandService                       | Domain Service 없음 (UseCase 직접 호출)               |
| Application 계층 | Facade + AuthService                                | UseCase only (Facade 제거)                        |
| @Transactional | Domain Service / Facade                             | UseCase                                          |
| Domain Model   | JPA Entity 겸용                                       | 순수 POJO (JPA Entity 완전 분리)                      |
| 검증 전략          | `guard()` (`@PrePersist`/`@PreUpdate`)              | `init { validate() }` + VO 자가 검증                |
| VO 전략          | `@Converter` 필요로 제한적 사용                             | `@JvmInline value class` 적극 활용                  |
| Repository     | `XxxJpaRepository` 별도 파일                            | `XxxRepositoryImpl.kt` 내 `internal interface`   |
| 테스트 전략         | E2E 위주                                              | 단위 테스트 중심 (Fake Repository)                     |
| Point 도메인      | 없음                                                  | UserPoint + PointHistory + PointDeductor + PointCharger |
