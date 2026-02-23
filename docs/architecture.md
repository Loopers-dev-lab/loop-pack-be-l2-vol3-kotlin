# Commerce API 아키텍처

## 레이어 구조 & 의존 방향

```
interfaces/  →  application/  →  domain/  ←  infrastructure/
```

**DIP**: `Application → Domain ← Infrastructure`. Domain은 어디에도 의존하지 않는다.

---

## 주요 설계 결정

1. **Aggregate Root 기반의 상태 변경 제어**:
    - `Order`와 `OrderItem`을 하나의 Aggregate로 묶고, `Order`를 Root로 지정함.
    - `OrderItem` 취소 등 상태 변경 시 `Order`의 `totalPrice` 등 파생 데이터 정합성이 깨지므로, **비즈니스 변경 진입점은 반드시 `Order`** 를 통한다.
    - Domain Service는 `OrderItem`을 직접 수정하지 않고, `Order`의 메서드(`cancelItem()` 등)를 통해 변경하며 Order 내부에서 재계산이 함께 처리된다.
    - JPA 연관관계를 사용하지 않으므로 영속성은 `OrderRepository` + `OrderItemRepository` 각각으로 처리하되, `OrderItemRepository`는 `OrderService`에서만 사용한다는 컨벤션으로 우회를 막음.

2. **이벤트 기반 분리 계획**: 현재는 비즈니스 로직의 응집도를 높일 수 있는 구조라 Facade를 채택하고 있으나, 추후 핵심 트랜잭션에 영향을 주지 않아야 하는 부가 기능이 추가될 경우
   Spring Application Event(`@EventListener`)를 혼합하여 트랜잭션 경계를 분리할 예정.

3. **Domain Model 생성 패턴 이원화**:
    - **`public constructor + init { validate() }`**: 독립적으로 생성 가능한 Domain Model. (Product, Brand, User, UserPoint 등)
    - **`private constructor + create()`**: 생성 시 구성 요소를 직접 조립하고 파생 값을 계산하는 Domain Model. 팩토리 메서드가 유일한 생성 경로. (Order — `Order.create()`는 내부에서 `OrderItem` 생성 + `totalPrice` 계산을 수행)

4. **Anti-Corruption Layer 적용**: `OrderProductInfo` 데이터 클래스를 통해 Order 도메인이 Product를 직접 참조하지 않도록 의존성을 단절함. Order 도메인이 필요한 정보만 자기 도메인의 데이터 클래스로 수신함.

5. **Domain Model ↔ JPA Entity 완전 분리**: JPA의 기술적 한계로 Domain Model이 훼손되지 않도록 순수 POJO Domain Model과 DB 매핑 전용 JPA Entity를 완전히 분리한다. 변환 책임은 JPA Entity(`fromDomain()` / `toDomain()`)가 전담하며 Domain Service는 Domain Model만 다룬다.

---

## Domain — 비즈니스 규칙의 핵심

### Domain Model: 도메인 비즈니스 (자기 상태를 스스로 변경/판단)

순수 POJO. JPA 애노테이션 없음. 비즈니스 규칙과 상태 변경을 스스로 수행하는 Rich Domain Model.

| Domain Model | 주요 메서드                                                                                  | 책임                                                                                                       |
|--------------|-----------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| `Product`    | `decreaseStock()`, `increaseStock()`, `update()`, `isActive()`, `isAvailableForOrder()` | 재고 증감 시 상태 자동 전환(ON_SALE↔SOLD_OUT), 상품 정보 수정, 주문 가능 여부 판단                                               |
| `Brand`      | `update()`, `isDeleted()`                                                               | 브랜드명 수정, 삭제 상태 판단                                                                                        |
| `User`       | `changePassword()`, `verifyPassword()`, `getMaskedName()`                               | 현재 비밀번호 검증 후 변경, 이름 마스킹                                                                                  |
| `UserPoint`  | `charge()`, `use()`, `canAfford()`                                                      | 잔액 충전/차감, 잔액 충분한지 판단. MAX_BALANCE 초과 방지                                                                  |
| `Order`      | `create()` (companion), `cancelItem()`                                                  | private constructor + 팩토리. `Order.create()`는 내부에서 `OrderItem` 생성 + `totalPrice` 계산 수행. `cancelItem()`으로 아이템 취소 시 totalPrice 재계산 |
| `OrderItem`  | `create()` (companion), `cancel()`                                                      | `OrderProductInfo`를 받아 생성. Product 직접 의존 차단. `cancel()`로 CANCELLED 상태 전환                                 |

### Domain Service: 애플리케이션 비즈니스 (Domain Model 협력 조율 + 교차 검증)

| Service                | 주요 검증/조율                                                                                  | Domain Model 위임                                                          |
|------------------------|-------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| `CatalogService`       | 상품 생성 시 **브랜드 존재 + 미삭제 확인**, 브랜드 삭제 시 **소속 상품 일괄 삭제**, 주문용 상품 조회 시 **전체 존재 + ON_SALE 검증** | `product.decreaseStock()`, `brand.update()` 등 상태 변경은 Domain Model에 위임 |
| `UserService`          | 회원가입 시 **loginId 중복 검증**                                                                  | `user.changePassword()`에 위임                                              |
| `PointChargingService` | **충전 금액 범위 검증** (0 < amount ≤ 10M), 비관적 락 획득                                              | `userPoint.charge()`에 위임 + PointHistory 기록                               |
| `UserPointService`     | 포인트 사용 시 비관적 락 획득                                                                         | `userPoint.use()`에 위임 + PointHistory 기록                                  |
| `OrderService`         | 주문 조회 시 **소유자 확인** (userId 일치 검증)                                                         | `Order.create()`로 생성 위임                                                  |
| `LikeService`          | **좋아요 중복 방지** (이미 존재하면 false 반환)                                                          | —                                                                        |

### 그 외 Domain 구성 요소

| 구성 요소              | 책임                  | 예시                                                         |
|--------------------|---------------------|------------------------------------------------------------|
| Repository (인터페이스) | 도메인 언어로 표현된 저장소 계약  | `findByUserIdForUpdate` (락 의도를 이름으로 표현)                    |
| Value Object       | 생성 시점에 자가 검증하는 불변 값 | `Stock`, `Point`, `LoginId`, `Password`, `Name`            |
| Command            | 서비스 요청 파라미터 묶음      | `CatalogCommand.CreateProduct`, `OrderCommand.CreateOrder` |

> **VO 도입 기준**: Domain Model이 순수 POJO이므로 `@Converter` 부담 없이 모든 도메인 값을 VO로 표현할 수 있다. 단일 값을 감싸는 VO는 `@JvmInline value class`로 선언하여 성능 오버헤드를 제거한다. 한 줄짜리 검증(`Price`, `Email`, `BrandName` 등)도 VO로 적극 표현한다.
> 복합 필드가 필요하면 `data class`, 도메인 메서드(`Stock.decrease()`, `Point.add()`)가 있으면 일반 `class`로 선언한다.

---

## Application — Cross-Domain 오케스트레이션

여러 Domain Service를 조합하는 **Facade**. 비즈니스 로직 자체는 Domain에 위임하고, 흐름만 조율.

| Facade        | 조합하는 서비스                                         | 하는 일                                   |
|---------------|--------------------------------------------------|----------------------------------------|
| `UserFacade`  | UserService + UserPointService                   | 회원가입: User 생성 → UserPoint 초기화          |
| `OrderFacade` | CatalogService + OrderService + UserPointService | 주문 생성: 상품 검증 → 재고 차감 → 주문 생성 → 포인트 차감  |
| `LikeFacade`  | LikeService + CatalogService                     | 좋아요: Like 등록/취소 + Product.likeCount 연동 |

---

## Infrastructure — Persistence Model & Repository 구현

Domain Model과 DB 사이의 변환을 전담한다.

**JPA Entity (Persistence Model)**: `XxxEntity` 클래스. DB 테이블 매핑 전용. 매핑 메서드를 보유한다:
- `companion object { fun fromDomain(domain: Xxx): XxxEntity }` — Domain Model → JPA Entity
- `fun toDomain(): Xxx` — JPA Entity → Domain Model

**Repository 구현**: `XxxRepositoryImpl.kt` 파일 하나에 `internal interface XxxJpaRepository`와 구현체 `XxxRepositoryImpl`을 함께 선언한다. JpaRepository는 구현 세부사항이므로 외부에 노출하지 않는다.

---

## Interfaces — API 진입점

Controller → Dto 변환 → Service/Facade 호출. OpenAPI 명세는 ApiSpec 인터페이스로 분리.

**특이사항:**

- **계층 뛰어넘기 허용**: 단일 도메인이면 Controller → Domain Service 직접 호출 (Facade 생략)
- **인증**: `AuthInterceptor`가 UserService를 직접 호출하여 헤더 기반 인증 처리
- **도메인 타입 변환**: `PageResult<T>.toSpringPage()` 확장 함수로 Controller에서 Spring 타입으로 변환
