# Round 3 — 도메인 모델링 & DDD 적용 요구사항

## 배경

Round 2에서 설계한 기능 요구사항(Product, Brand, Like, Order)을 **DDD 관점으로 재구성**한다.
새로운 API 기능 추가보다는 **도메인 객체의 책임 분리, 비즈니스 규칙 캡슐화, 테스트 가능한 구조**에 초점을 맞춘다.

추가로, 강의에서 제시된 **포인트 시스템**을 도입하여 주문 흐름에 결제 개념을 포함한다.

> 기능 요구사항(API 명세, 예외 흐름 등)은 [Round 2 요구사항](round2-requirements-analysis.md)을 기준으로 한다.
> 본 문서는 Round 2 대비 **변경/추가된 설계 결정**만 다룬다.

---

## Round 2 대비 변경사항 요약

| 구분                | Round 2                          | Round 3                                                                    |
|-------------------|----------------------------------|----------------------------------------------------------------------------|
| 결제                | "추후 추가 개발 예정"                    | 포인트 기반 결제 도입 (UserPoint + PointHistory)                                    |
| Application Layer | Facade가 cross-domain 오케스트레이션     | UseCase only — Facade 전면 제거. UseCase가 Repository/Domain Service를 직접 주입받아 호출 |
| 상품 상세 조합          | ProductFacade.getProductDetail() | `GetProductUseCase` — UseCase가 ProductRepository + BrandRepository 직접 조합      |
| 바운디드 컨텍스트         | Product, Brand 별도 도메인            | Catalog 컨텍스트로 통합 (Product + Brand)                                         |
| 테스트 전략            | E2E 위주                           | 단위 테스트 중심 (Fake/Stub 기반 도메인 로직 검증)                                         |
| VO 범위             | Stock, Price, BrandName          | Stock, Point 유지 + Price, BrandName 등 `@JvmInline value class`로 적극 활용       |
| 주문 흐름             | 재고 차감만                           | 재고 차감 + 포인트 차감                                                             |

---

## 1. 문제 정의

### DDD 관점에서의 핵심 문제

| 관점   | 문제                                                                                                     |
|------|--------------------------------------------------------------------------------------------------------|
| 설계   | 비즈니스 로직이 Service에 집중되면 도메인 객체가 빈혈 모델(Anemic Model)이 된다. Domain Model과 VO가 자신의 규칙을 스스로 검증하고 행위를 수행해야 한다 |
| 아키텍처 | Application Layer가 비대해지면 도메인 로직의 재사용이 어려워진다. Domain Service를 도입하여 도메인 간 협력 로직을 분리해야 한다                 |
| 테스트  | 외부 의존성(DB, Redis)과 결합된 구조에서는 단위 테스트가 불가능하다. DIP를 통해 Repository를 인터페이스로 분리하고 Fake/Stub으로 대체해야 한다        |

### 포인트 시스템 도입 배경

| 관점   | 문제                                                                        |
|------|---------------------------------------------------------------------------|
| 사용자  | 상품을 주문하려면 결제 수단이 필요하다. 포인트를 충전하고 이를 차감하여 상품을 구매하는 흐름을 제공한다                |
| 비즈니스 | 포인트 충전과 상품 결제를 분리하면, 추후 "포인트 충전에 대한 결제" 모듈만 붙이면 된다. 상품 주문 로직은 변경 없이 유지된다  |
| 시스템  | 포인트 잔액 변경과 충전 내역은 원자적으로 처리되어야 한다. 잔액 부족 시 주문 전체가 실패해야 한다 (all-or-nothing) |

---

## 2. 유비쿼터스 언어 (추가)

Round 2 유비쿼터스 언어에 다음 용어를 추가한다.

| 한글         | 영문                   | 정의                                                                             |
|------------|----------------------|--------------------------------------------------------------------------------|
| 유저 포인트     | UserPoint            | 사용자의 포인트 잔액을 관리하는 Domain Model. User와 1:1 관계                                   |
| 포인트        | Point                | 포인트 금액을 나타내는 Value Object. 0 이상이어야 하며, 연산 시 음수 방지를 보장                          |
| 포인트 내역     | PointHistory         | 포인트 충전/차감 이력. 충전(CHARGE)과 사용(USE) 유형을 가진다                                      |
| 포인트 충전기    | PointCharger         | 포인트 충전을 처리하는 Domain Service. 잔액 변경 + 내역 생성의 원자적 보장                               |
| 수량         | Quantity             | 주문 항목의 수량을 나타내는 Value Object (`@JvmInline value class`). 1 이상이어야 한다             |
| 카탈로그       | Catalog              | Product와 Brand를 포함하는 바운디드 컨텍스트. 상품 탐색/조회에 필요한 정보를 하나의 경계로 관리                   |
| 포인트 차감기    | PointDeductor        | 포인트 결제를 처리하는 Domain Service. 잔고 차감 + 이력 생성의 원자적 보장                                |
| 도메인 서비스    | Domain Service       | 상태를 갖지 않고, **동일한 도메인 경계 내**의 도메인 객체 간 협력을 조율하는 서비스. 단일 Entity가 수행하기 어려운 로직을 담당 |
| 회원가입 유스케이스 | RegisterUserUseCase  | 회원가입 시 UserRepository + UserPointRepository를 조합하는 UseCase                     |

---

## 3. 신규 도메인

### 3.1 UserPoint (유저 포인트)

사용자의 포인트 잔액을 별도 Domain Model로 관리한다. User에 balance 필드를 추가하지 않는다.

**분리 이유:**

- User의 책임 과다 방지 (인증 + 프로필 + 잔액이 하나의 Domain Model에 몰리면 변경 이유가 너무 많음)
- 포인트 관련 로직(충전, 차감, 이력)을 독립적으로 발전시킬 수 있음
- 추후 포인트와 결제가 별도 바운디드 컨텍스트로 분리될 가능성

**Domain Model 구조:**

| 필드        | 타입   | 설명                                 |
|-----------|------|------------------------------------|
| id        | Long | PK                                 |
| refUserId | Long | User 참조 (1:1, 논리FK)                |
| balance   | Long | 현재 포인트 잔액 (init 블록에서 Point VO로 검증) |

**행위:**

| 메서드                                | 설명                                            |
|------------------------------------|-----------------------------------------------|
| `charge(amount: Long)`             | 포인트 충전 (잔액 증가). 내부에서 Point VO를 통해 검증          |
| `use(amount: Long)`                | 포인트 사용 (잔액 차감, 부족 시 예외). 내부에서 Point VO를 통해 검증 |
| `canAfford(amount: Long): Boolean` | 잔액 충분 여부 확인                                   |

**생성 시점:** 회원가입 시 UserPoint를 함께 생성한다 (초기 잔액 0).

### 3.2 PointHistory (포인트 내역)

포인트 변동 이력을 기록한다. 감사(Audit) 목적과 향후 포인트 사용 내역 조회에 활용한다.

**Domain Model 구조:**

| 필드             | 타입               | 설명                              |
|----------------|------------------|---------------------------------|
| id             | Long             | PK                              |
| refUserPointId | Long             | UserPoint 참조 (논리FK)             |
| type           | PointHistoryType | CHARGE / USE                    |
| amount         | Long             | 변동 금액 (init에서 amount > 0 직접 검증) |
| refOrderId     | Long?            | 추적용. USE 시 주문 ID, CHARGE 시 null |
| createdAt      | ZonedDateTime    | 발생 시각                           |

**PointHistoryType (PointHistory 내부 enum class):**

| 값      | 설명             |
|--------|----------------|
| CHARGE | 포인트 충전         |
| USE    | 포인트 사용 (주문 차감) |

> BaseEntity를 상속하지 않는다. 내역은 불변(immutable) 데이터이므로 updatedAt, deletedAt이 불필요하다.
> id + createdAt만 필요하며, soft delete도 적용하지 않는다.

---

## 4. 도메인 규칙 (추가/변경)

### 4.1 포인트 규칙

- 포인트 잔액(Point VO)은 0 이상이어야 한다
- 포인트 충전 금액은 1 이상이어야 한다
- 포인트 사용 시 잔액이 부족하면 `CoreException(BAD_REQUEST)` 발생
- 포인트 충전은 잔액 변경(UserPoint 수정) + 충전 내역(PointHistory 생성)을 하나의 트랜잭션에서 처리한다
- 이 복합 로직은 `PointCharger` (Domain Service)가 조율한다
- PointHistory는 추적용 필드 `refOrderId`(nullable)를 가진다. 주문으로 인한 포인트 사용(USE) 시 해당 주문 ID를 기록하며, 충전(CHARGE) 시에는 null이다

### 4.2 주문 흐름 변경

Round 2의 주문 흐름에 포인트 차감이 추가된다.

**변경된 주문 처리 흐름:**

1. 시스템이 각 상품의 존재 여부 및 판매 가능 상태를 확인한다
2. 시스템이 각 상품의 재고를 확인하고 차감한다 (all-or-nothing)
    - 재고 차감 후 stock == 0이면 status → `SOLD_OUT` 자동 전환
3. 시스템이 주문 시점의 상품 정보(이름, 가격)를 스냅샷으로 저장한다
4. **시스템이 주문 총액을 계산하고, 사용자의 포인트 잔액을 확인 후 차감한다**
    - **포인트 부족 시 주문 전체 실패 (재고 차감도 롤백)**
5. **포인트 사용 내역(PointHistory, type=USE)을 기록한다**
6. 주문이 생성되고 (status: CREATED) 사용자에게 응답한다

**추가된 예외 흐름:**

| 조건        | 응답  | 설명                      |
|-----------|-----|-------------------------|
| 포인트 잔액 부족 | 400 | 전체 실패, 필요 포인트와 현재 잔액 명시 |

### 4.3 도메인 경계 정의

각 도메인의 경계(Bounded Context)를 명시한다. Domain Service는 여러 Entity 간 원자적 얽힘이 있는 경우에만 사용한다. 모든 오케스트레이션(단일 도메인이든 cross-domain이든)은 UseCase가 담당한다.

| 도메인 경계      | 포함 객체                                                                       | 근거                                                      |
|-------------|-----------------------------------------------------------------------------|---------------------------------------------------------|
| **User**    | User                                                                        | 인증/프로필 책임                                               |
| **Catalog** | Product, Brand, Stock(VO), ProductStatus                                    | 상품 탐색에 필요한 정보를 하나의 경계로 관리. 브랜드와 상품은 함께 조회/관리되므로 동일 컨텍스트 |
| **Like**    | Like                                                                        | 사용자-상품 관심 표현                                            |
| **Order**   | Order, OrderItem, OrderProductInfo, OrderStatus                             | 주문 생명주기                                                 |
| **Point**   | UserPoint, PointHistory, Point(VO), PointHistoryType (PointHistory 내부 enum) | 포인트 잔액/이력 관리                                            |

**도메인 경계에 따른 서비스 배치:**

| 조합                           | 경계 판단           | 서비스 위치                                  |
|------------------------------|-----------------|-----------------------------------------|
| Product + Brand (상세 조합)      | 같은 경계 (Catalog) | UseCase (`GetProductUseCase`) — Repository 직접 호출  |
| UserPoint + PointHistory     | 같은 경계 (Point)   | Domain Service (`PointCharger`, `PointDeductor`)  |
| Brand + Product (삭제 cascade) | 같은 경계 (Catalog) | UseCase (`DeleteBrandUseCase`) — Repository 직접 호출 |
| Brand + Product (등록 검증)      | 같은 경계 (Catalog) | UseCase (`CreateProductUseCase`) — Repository 직접 호출 |
| User + UserPoint             | 다른 경계           | UseCase (`RegisterUserUseCase`)                     |
| Product + UserPoint + Order  | 다른 경계           | UseCase (`PlaceOrderUseCase`)                       |
| Like + Product               | 다른 경계           | UseCase (`AddLikeUseCase`, `GetUserLikesUseCase`)   |

### 4.4 Domain Service 구조

Domain Service는 여러 Entity 간 원자적 얽힘이 있는 경우에만 생성한다. 단순 CRUD는 UseCase가 Repository를 직접 호출한다.
`@Component`로 등록하여 DI를 활용한다.

| Domain Service  | 경계    | 책임                              | Repository                                  |
|-----------------|-------|---------------------------------|---------------------------------------------|
| `PointDeductor` | Point | 포인트 결제 — 잔고 차감 + 이력 생성의 원자적 보장 | UserPointRepository, PointHistoryRepository |
| `PointCharger`  | Point | 포인트 충전 — 잔액 변경 + 내역 생성의 원자적 보장 | UserPointRepository, PointHistoryRepository |

> Catalog, Like, Order, User 도메인에는 Domain Service가 없다. UseCase가 Repository를 직접 호출한다.

**Domain Service vs UseCase 역할 분담:**

| 기준            | Domain Service                                  | UseCase                                          |
|---------------|-------------------------------------------------|--------------------------------------------------|
| 위치            | domain 패키지                                      | application 패키지                                  |
| 역할            | 여러 Entity 간 원자적 얽힘 보장                           | 오케스트레이션 (단일/cross-domain 모두)                     |
| 예시            | `PointDeductor` (잔고 차감 + 이력 생성), `PointCharger` | `PlaceOrderUseCase`, `RegisterUserUseCase`       |
| Repository 접근 | 직접 접근 (@Component)                               | 직접 접근 (기본 패턴)                                    |
| 트랜잭션          | 없음 (UseCase에 위임)                                | 유스케이스 단위 @Transactional                           |

---

## 5. 아키텍처 결정

### 5.1 레이어 구조 (DIP 적용)

```
interfaces/api/     → Controller, ApiSpec, Dto
application/        → UseCase (트랜잭션 경계, 오케스트레이션)
domain/             → Domain Model(순수 POJO), VO, Domain Service(@Component), Repository(인터페이스)
infrastructure/     → XxxEntity(JPA), XxxRepositoryImpl(internal JpaRepository 포함)
```

**의존 방향:** `Application → Domain ← Infrastructure`

- Domain 계층은 다른 계층에 의존하지 않는다
- Repository 인터페이스는 Domain에 정의, 구현체는 Infrastructure에 위치
- UseCase는 필요한 Repository와 Domain Service를 직접 주입받아 유스케이스를 완성

### 5.2 패키지 구조

```
com.loopers/
├── interfaces/api/
│   ├── user/
│   ├── brand/
│   ├── product/
│   ├── like/
│   ├── order/
│   └── point/              ← 신규
├── application/
│   ├── user/               ← 신규 (RegisterUserUseCase 등)
│   ├── catalog/            ← 신규 (brand/, product/ 하위 UseCase)
│   ├── order/
│   ├── like/
│   └── point/              ← 신규 (ChargePointUseCase, GetUserPointUseCase)
├── domain/
│   ├── user/
│   ├── catalog/            ← 신규 (Product + Brand 통합)
│   │   ├── ProductDetail.kt
│   │   ├── product/
│   │   │   ├── Product.kt
│   │   │   └── ProductRepository.kt
│   │   └── brand/
│   │       ├── Brand.kt
│   │       └── BrandRepository.kt
│   ├── like/
│   ├── order/
│   └── point/              ← 신규
│       ├── UserPoint.kt
│       ├── PointHistory.kt
│       ├── Point.kt (VO)
│       ├── PointCharger.kt
│       ├── PointDeductor.kt
│       ├── UserPointRepository.kt
│       └── PointHistoryRepository.kt
└── infrastructure/
    ├── user/
    ├── catalog/            ← 신규
    │   ├── product/
    │   └── brand/
    ├── like/
    ├── order/
    └── point/              ← 신규
```

### 5.3 Facade 제거 — UseCase 직접 호출 구조

Round 2의 Facade 패턴을 전면 제거하고, UseCase가 필요한 Repository와 Domain Service를 직접 주입받아 호출한다.

| 기존 (Round 2)                       | 변경 (Round 3)                                        | 이유                                         |
|------------------------------------|-----------------------------------------------------|--------------------------------------------|
| `ProductFacade.getProductDetail()` | `GetProductUseCase` (Repository 직접 호출)              | UseCase가 ProductRepository + BrandRepository 직접 조합 |
| `ProductFacade.createProduct()`    | `CreateProductUseCase` (Repository 직접 호출)           | UseCase가 브랜드 검증 후 상품 생성                    |
| `BrandFacade.deleteBrand()`        | `DeleteBrandUseCase` (Repository 직접 호출)             | UseCase가 cascade soft delete 처리             |
| `OrderFacade.createOrder()`        | `PlaceOrderUseCase` (Repository + PointDeductor 조합) | UseCase가 cross-domain 오케스트레이션               |
| `LikeFacade`                       | `AddLikeUseCase`, `RemoveLikeUseCase` 등             | UseCase가 Like + Product 조합                  |
| —                                  | `RegisterUserUseCase` (신규)                          | UseCase가 User + UserPoint 생성                |

**Facade 전면 제거.** 모든 오케스트레이션은 UseCase가 담당한다.

---

## 6. VO 정리

### 기존 유지

| VO    | 소속 도메인            | 검증 규칙                      |
|-------|-------------------|----------------------------|
| Stock | Catalog (Product) | Int >= 0, decrease 시 부족 확인 |

> BrandName, Price VO는 `@JvmInline value class`로 부활 -- Domain Model이 순수 POJO이므로 `@Converter` 부담 없이 모든 도메인 값을 VO로 표현할 수
> 있다.

### 신규 추가

| VO    | 소속 도메인            | 검증 규칙     | 행위                                   |
|-------|-------------------|-----------|--------------------------------------|
| Point | point (UserPoint) | Long >= 0 | plus, minus, isGreaterThanOrEqual 연산 |

| Quantity | common (OrderItem 등에서 사용) | Int >= 1 | — (`@JvmInline value class`) |

> Domain Model은 순수 POJO이므로 VO를 필드 타입으로 직접 사용할 수 있다. JPA Entity는 DB 컬럼 타입(String, BigDecimal 등)으로 저장하고, `toDomain()`에서
> VO로 복원한다.

---

## 7. API 변경사항

### 7.1 포인트 관련 API (신규)

| METHOD | URI                           | 인증 | 설명          |
|--------|-------------------------------|----|-------------|
| POST   | `/api/v1/users/points/charge` | O  | 포인트 충전      |
| GET    | `/api/v1/users/points`        | O  | 내 포인트 잔액 조회 |

**포인트 충전 요청 파라미터:**

`@RequestParam amount: Long` — 쿼리 파라미터 방식

```
POST /api/v1/users/points/charge?amount=50000
```

> **향후 전환 노트:** idempotencyKey 등 필드가 추가되면 `@RequestBody` JSON 방식으로 전환해야 한다.

**포인트 충전 예외 흐름:**

| 조건          | 응답  | 설명          |
|-------------|-----|-------------|
| 인증 헤더 누락    | 401 | 메시지 통일      |
| 인증 실패       | 401 | 메시지 통일      |
| 충전 금액이 0 이하 | 400 | amount >= 1 |

**포인트 잔액 조회 응답:**

```json
{
  "balance": 50000
}
```

### 7.2 기존 API 변경 없음

Round 2의 모든 API 명세는 그대로 유지한다.
주문 생성 API의 요청/응답 형태는 동일하되, 내부 처리 흐름에 포인트 차감이 추가된다.

### 7.3 어드민 Restore API (신규)

| METHOD | URI                                          | 인증   | 설명     |
|--------|----------------------------------------------|------|--------|
| POST   | `/api-admin/v1/brands/{brandId}/restore`     | LDAP | 브랜드 복구 |
| POST   | `/api-admin/v1/products/{productId}/restore` | LDAP | 상품 복구  |

**예외 흐름:**

| 조건         | 응답  | 설명        |
|------------|-----|-----------|
| 존재하지 않는 대상 | 404 | NOT_FOUND |

**멱등성:** 이미 활성(미삭제) 상태인 대상을 복구해도 200 OK를 반환한다.

### 7.4 인증 경로 추가

| 경로 패턴                     | 인터셉터            | 비고                                  |
|---------------------------|-----------------|-------------------------------------|
| `/api/v1/users/points/**` | AuthInterceptor | 기존 `/api/v1/users/**` 패턴에 의해 자동 포함됨 |

---

## 8. 테스트 전략

### 8.1 단위 테스트 (Round 3 핵심)

도메인 로직의 정합성을 외부 의존성 없이 검증한다.

**테스트 대상:**

| 대상                                         | 검증 내용                    | 테스트 방식             |
|--------------------------------------------|--------------------------|--------------------|
| Domain Model (Product, Order, UserPoint 등) | 비즈니스 메서드, init 검증, 상태 전이 | 순수 단위 테스트 (의존성 없음) |
| VO (Point, Stock 등)                        | 자가 검증, 연산 규칙             | 순수 단위 테스트          |
| Domain Service                             | 비즈니스 로직, 도메인 객체 간 협력     | Fake Repository 주입 |

**Fake/Stub 전략:**

- Repository 인터페이스에 대한 `FakeXxxRepository`를 작성하여 인메모리로 동작하게 한다
- `@SpringBootTest` 없이 도메인 로직을 검증한다
- E2E 테스트는 기존 방식(TestContainers) 유지, 단위 테스트와 병행

### 8.2 테스트 검증 항목 (퀘스트 체크리스트 기반)

**Product/Brand:**

- [ ] 상품 재고 차감 시 음수 방지 (도메인 레벨)
- [ ] 상품 상태 자동 전이 (stock 변경 → status 변경)
- [ ] 상품 상세 조합 (Product + Brand + likeCount)

**Like:**

- [ ] 좋아요 등록/취소 흐름 (멱등성 포함)
- [ ] 삭제된 상품에 대한 좋아요 처리

**Order:**

- [ ] 정상 주문 흐름 (재고 차감 + 포인트 차감 + 스냅샷 저장)
- [ ] 재고 부족 시 전체 실패
- [ ] 포인트 부족 시 전체 실패
- [ ] 주문 항목 중복 상품 방지

**Point:**

- [ ] 포인트 충전 (잔액 증가 + 내역 생성)
- [ ] 포인트 사용 (잔액 차감 + 내역 생성)
- [ ] 잔액 부족 예외

---

## 9. 잠재 리스크

| 리스크                       | 영향                                                       | 현재 대응                                                               | 향후 대응                                       |
|---------------------------|----------------------------------------------------------|---------------------------------------------------------------------|---------------------------------------------|
| **주문 트랜잭션 비대화**           | 재고 차감 + 포인트 차감 + 주문 생성 + 스냅샷 + 내역 기록이 하나의 트랜잭션 → 락 경합 증가 | 단일 트랜잭션으로 처리 (현재 규모)                                                | 이벤트 기반 분리 (재고 선점 → 포인트 차감 → 주문 확정)          |
| **포인트 정합성**               | 동시 주문/충전 시 잔액 불일치 가능                                     | 단일 트랜잭션 내 처리                                                        | Optimistic Lock (@Version) 또는 Atomic UPDATE |
| **Catalog UseCase 비대화**   | Catalog 경계의 UseCase 수가 증가하면 패키지 복잡도가 높아질 수 있음          | 복잡도는 도메인 컴포넌트(Entity, VO)로 분산. UseCase는 단일 책임 유지                             | 정기 리뷰로 경계 재조정. 비대화 시 Catalog 경계 자체를 분리 검토   |
| **Fake Repository 유지 비용** | Repository 인터페이스 변경 시 Fake도 함께 수정 필요                     | 테스트 코드의 투자로 감수                                                      | Fake 생성 자동화 검토                              |

---

## 10. 설계 결정 사항

### UserPoint를 User와 분리하는 이유

- **결정**: User에 balance 필드를 추가하지 않고, UserPoint 별도 Domain Model로 관리한다
- **근거**: User는 인증/프로필 책임, UserPoint는 잔액 관리 책임. SRP(단일 책임 원칙) 준수. 추후 포인트를 별도 바운디드 컨텍스트로 분리할 때 유리하다

### PointHistory를 별도 Domain Model로 관리하는 이유

- **결정**: 포인트 변동마다 PointHistory 레코드를 생성한다
- **근거**: 충전/사용 이력을 추적할 수 있어야 한다. UserPoint.balance만으로는 "언제, 왜, 얼마나" 변동했는지 알 수 없다. 감사(Audit) 목적으로도 필수

### 포인트 충전을 Domain Service로 분리하는 이유

- **결정**: `PointCharger`가 잔액 변경(UserPoint) + 내역 생성(PointHistory)을 조율한다. 포인트 사용(차감)은 `PointDeductor`가 담당한다
- **근거**: 충전/사용 모두 단일 Entity의 메서드 호출로 완결되지 않는다. UserPoint 상태 변경과 PointHistory 생성이 반드시 함께 수행되어야 하므로, 이 원자적 얽힘을 Domain Service가
  보장한다

### Catalog 바운디드 컨텍스트 도입

- **결정**: Product와 Brand를 별도 도메인이 아닌 하나의 Catalog 바운디드 컨텍스트로 통합한다
- **근거**: 상품 탐색 시 브랜드 정보는 필수적으로 함께 제공된다. 대고객 API(상품 목록, 상품 상세, 브랜드 조회) 모두 Catalog 내에서 해결 가능하다. UseCase가 같은 경계 내의 Repository를 직접 조합하므로 Facade가 불필요하다
- **효과**: UseCase가 ProductRepository + BrandRepository를 직접 주입받아 처리. 상품 상세 조합, 등록 시 브랜드 검증, 삭제 시 상품 cascade를 UseCase 내에서 수행. 복잡도는 도메인 컴포넌트(Entity, VO)로 분산

### Like를 Catalog에 포함하지 않는 이유

- **결정**: Like는 Catalog와 별도 도메인으로 유지한다
- **근거**: Like는 User-Product 관계로, 순수 카탈로그 정보가 아닌 "사용자의 관심 표현"이다. likeCount는 Product에 비정규화된 값이고 실제 데이터는 Like 도메인에 있다. 향후
  추천/랭킹 시스템의 기반 데이터로 확장 가능성이 있으며, Catalog에 포함 시 변경 이유가 달라 응집도가 떨어진다

### Domain Service에 @Component를 사용하는 이유

- **결정**: Domain Service도 Spring `@Component`로 등록하여 DI를 활용한다
- **근거**: 기존 프로젝트 패턴과의 일관성. Repository 접근이 필요한 Domain Service(`PointDeductor`, `PointCharger`)는 DI 없이는 사용이 불편하다. 순수
  DDD 이론보다 실용성을 우선한다

### 결제 시스템 분리 전략

- **결정**: "상품 결제"가 아닌 "포인트 충전에 대한 결제"로 설계한다
- **근거**: 추후 PG사 연동 시 포인트 충전 API에만 결제 모듈을 붙이면 된다. 상품 주문 로직(재고 차감 + 포인트 차감)은 결제 방식에 무관하게 동일하다. 결합도를 최소화하는 전략이다

---

## 체크리스트

### Product / Brand 도메인

- [ ] 상품 정보 객체는 브랜드 정보, 좋아요 수를 포함한다
- [ ] 상품의 정렬 조건(latest, price_asc, likes_desc)을 고려한 조회 기능을 설계했다
- [ ] 상품은 재고를 가지고 있고, 주문 시 차감할 수 있어야 한다
- [ ] 재고의 음수 방지 처리는 도메인 레벨에서 처리된다

### Like 도메인

- [ ] 좋아요는 유저와 상품 간의 관계로 별도 도메인으로 분리했다
- [ ] 상품의 좋아요 수는 상품 상세/목록 조회에서 함께 제공된다
- [ ] 단위 테스트에서 좋아요 등록/취소 흐름을 검증했다

### Order 도메인

- [ ] 주문은 여러 상품을 포함할 수 있으며, 각 상품의 수량을 명시한다
- [ ] 주문 시 상품의 재고 차감, 유저 포인트 차감을 수행한다
- [ ] 재고 부족, 포인트 부족 등 예외 흐름을 고려해 설계되었다
- [ ] 단위 테스트에서 정상 주문 / 예외 주문 흐름을 모두 검증했다

### Point 도메인 (신규)

- [ ] UserPoint Domain Model이 User와 분리되어 별도 관리된다
- [ ] 포인트 충전 시 잔액 변경 + 내역 생성이 함께 처리된다
- [ ] 포인트 사용 시 잔액 부족 예외가 도메인 레벨에서 처리된다
- [ ] 단위 테스트에서 충전/사용/부족 흐름을 검증했다

### 도메인 서비스

- [ ] 도메인 내부 규칙은 Domain Service에 위치시켰다
- [ ] 상품 상세 조회 시 Product + Brand 정보 조합은 Application Layer에서 처리했다
- [ ] 복합 유스케이스는 Application Layer에 존재하고, 도메인 로직은 위임되었다
- [ ] 도메인 서비스는 상태 없이, 동일한 도메인 경계 내의 도메인 객체의 협력 중심으로 설계되었다

> **주의 — 체크리스트 원문과의 차이:**
>
> 체크리스트 원문: "상품 상세 조회 시 Product + Brand 정보 조합은 Application Layer에서 처리했다"
>
> 본 설계의 결정: Catalog 바운디드 컨텍스트를 도입하여 Product + Brand 조합을 **UseCase(`GetProductUseCase`)** 에서 처리한다.
> 조합 결과물(`ProductDetail`)은 `domain/catalog/` 패키지에 위치한다.
>
> **변경 근거:** Product와 Brand는 같은 Catalog 경계에 속하므로 조합 결과물도 domain 레이어에 둔다.
> UseCase가 domain 레이어의 `ProductDetail`을 조립하여 반환하므로 DIP를 위반하지 않는다.
>
> **Info 객체 배치 원칙:**
> - 같은 BC 내 조합 → domain 레이어 데이터 클래스 (예: `domain/catalog/ProductDetail`)
> - BC 간 조합 (UseCase 경유) → application 레이어 Info 객체 (예: `application/like/LikeInfo`)

### 소프트웨어 아키텍처 & 설계

- [ ] 전체 프로젝트의 구성은 Application → Domain ← Infrastructure 아키텍처를 기반으로 구성되었다
- [ ] Application Layer는 도메인 객체를 조합해 흐름을 orchestration 했다
- [ ] 핵심 비즈니스 로직은 Domain Model, VO, Domain Service에 위치한다
- [ ] Repository Interface는 Domain Layer에 정의되고, 구현체는 Infra에 위치한다
- [ ] 패키지는 계층 + 도메인 기준으로 구성되었다
- [ ] 테스트는 외부 의존성을 분리하고, Fake/Stub 등을 사용해 단위 테스트가 가능하게 구성되었다

---

## 11. 어드민/대고객 권한 경계 (추가 정의)

코드 리뷰에서 권한 경계 미정의로 인한 버그가 발견되어, 각 도메인 객체의 상태별 접근 권한을 명시한다.

### 11.1 원칙

- 대고객 API: 활성(정상) 데이터만 접근 가능. 삭제/HIDDEN 대상은 404 반환
- 어드민 API: 모든 상태의 데이터에 접근 가능. 조회/수정/복구 모두 허용
- 타인 리소스 접근: 404 반환 (리소스 존재 여부를 노출하지 않음)

### 11.2 Soft Delete 정책

- soft delete된 대상은 어드민이 복구할 수 있다
- 복구 API: `POST /api-admin/v1/{entity}/{id}/restore`
- 브랜드 삭제 시 소속 상품은 cascade soft delete되지만, 복구는 개별 수행
- soft delete 목적: 감사 로그, 참조 무결성 보호, 어드민 복구

**Cascade 복구 정책:**

- 브랜드 복구 시 해당 브랜드에 소속된 상품은 자동 복구되지 않는다
- 어드민이 각 상품을 개별적으로 복구해야 한다
- 이유: 브랜드 삭제로 인한 상품 cascade 삭제가 의도적이었는지를 어드민이 판단할 수 있도록 함

### 11.3 대고객 좋아요 목록

- 좋아요한 상품이 삭제/HIDDEN 되면 목록에서 자동 필터링 (사용자 알림 없음)
- 이는 의도된 동작: 비활성 상품 정보를 대고객에게 노출하지 않는 정책과 일관


