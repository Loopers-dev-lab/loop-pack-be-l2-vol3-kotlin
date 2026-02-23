# Domain 레이어

비즈니스 규칙의 핵심. 다른 레이어에 의존하지 않는다.

## Domain Model — Rich POJO

순수 POJO. JPA 애노테이션 없음. 비즈니스 규칙과 상태 변경을 스스로 수행한다.

**생성 패턴 선택 기준:**

- **독립적 생성** (Product, Brand, User, UserPoint): `public constructor` + `init { validate() }`
- **조립+파생값 계산** (Order): `private constructor` + `companion object { create() }`. 팩토리 메서드가 유일한 생성 경로.

**설계 원칙:**

- 검증, 상태 변경, 상태 판단은 Domain Model 내부에서 수행 (예: `Product.isActive()`, `UserPoint.canAfford()`)
- 규칙이 여러 Service에 나타나면 도메인 객체에 속할 가능성이 높다
- cross-domain 타입 의존 방지: 다른 도메인 모델을 직접 받지 않고, 자기 도메인의 데이터 클래스로 필요한 정보만 수신 (예: `OrderProductInfo`)

## Value Object

| 유형 | 선언 방식 | 예시 |
|---|---|---|
| 단일 값 감싸기 | `@JvmInline value class` | `Price`, `Email`, `LoginId` |
| 도메인 메서드 보유 | 일반 `class` | `Stock.decrease()`, `Point.add()` |
| 복합 필드 | `data class` | `Address` |

- 생성 시점에 자가 검증 (`init` 블록에서 규칙 위반 시 `CoreException` throw)
- 한 줄짜리 검증도 VO로 적극 표현

## Command

서비스 호출 시 요청 파라미터를 `XxxCommand` class 내부에 data class로 묶는다 (예: `CatalogCommand.CreateProduct`). Controller에서 Dto → Command 변환 후 서비스에 전달.

## Repository 인터페이스

- **도메인 언어와 기본 타입만** 사용. Spring Data 타입(`Pageable`, `Page`) 노출 금지.
- 페이지네이션: `page: Int, size: Int` 파라미터 + `PageResult<T>(data, totalElements, page, size)`
- 락 의도를 이름으로 표현 (예: `findByUserIdForUpdate`)

## Domain Service

`@Service` 또는 `@Component`로 등록. Repository를 통해 Domain Model을 조회/저장하고 비즈니스 로직을 수행한다. 단일 도메인 CRUD(CatalogService)부터 복수 도메인 객체 협력(PointChargingService)까지 도메인 레이어에서 처리한다.

**Domain Model은 절대적 순수성 유지. Domain Service만 Spring DI 타협 허용.**

## Aggregate Root

- `Order`와 `OrderItem`은 하나의 Aggregate. `Order`가 Root.
- 비즈니스 변경 진입점은 반드시 `Order`를 통한다 (예: `cancelItem()` → 내부에서 totalPrice 재계산).
- `OrderItemRepository`는 `OrderService`에서만 사용한다는 컨벤션으로 우회를 막는다.
