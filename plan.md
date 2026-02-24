# 통합 코드 리뷰 반영 Plan

## Context

Architect + Quality Reviewer + Gemini 통합 리뷰 결과 18개 항목에 대한 개발자 의사결정을 반영한다.
핵심 변경: (1) Order Aggregate Root 패턴 강화, (2) Point VO 통합 + 네이밍 개선, (3) Like 동시성 해결, (4) UseCase 분리/정리, (5) 버그 수정.

## 범위 요약

| # | 항목 | 분류 |
|---|------|------|
| 1 | likeCount 동시성 — findByIdForUpdate | 버그 |
| 3 | UpdateProductUseCase valueOf → BAD_REQUEST | 버그 |
| 4 | GetBrandUseCase → GetBrandUseCase + GetBrandAdminUseCase 분리 | 구조 |
| 5 | findItemsByOrders CQRS → round3-decisions.md 기록 | 문서 |
| 6 | 조회 UseCase @Transactional(readOnly=true) 추가 (7개) | 구조 |
| 8 | JpaRepository internal 키워드 (8개) | 구조 |
| 9 | LikeFacadeTest → LikeUseCaseTest 리네이밍 | 구조 |
| 10 | Order Aggregate Root — Order.create가 items 생성 + totalPrice 계산, items 필드 보유 | 구조 |
| 11 | RegisterUserUseCase DataIntegrityViolationException 처리 | 버그 |
| 12 | `!!` 연산자 제거 (PlaceOrderUseCase — CP2에서 자연 해소) | 구조 |
| 13 | ChargePointUseCaseTest 추가 | 테스트 |
| 14 | DeleteBrandUseCase 이미 삭제된 브랜드 가드 | 버그 |
| 15 | 도메인 모델 정리 — Like/PointHistory val, guard() 인라인, PointHistory amount→Point | 구조 |
| 16 | Point VO 통합 — PointCharger/UserPoint Point 파라미터, PointPaymentProcessor→PointPayer | 구조 |
| 7 | CLAUDE.md 문서 동기화 | 문서 |

스킵: #2(중복 productId — 의도된 설계), #17(ProductEntity.status — 문제없음), #18(LikeEntity BaseEntity — 의도됨)

---

## CP1: Point 도메인 리팩토링 (Structural)

모든 테스트가 통과하는 상태에서 구조만 변경. 기존 테스트를 새 시그니처에 맞춰 수정.

### 1-1. Like 도메인 모델 — 불변 필드 전환

**파일**: `domain/like/model/Like.kt`

- `var refUserId private set` → `val refUserId`
- `var refProductId private set` → `val refProductId`
- 생성 후 절대 변경되지 않으므로 val이 정확

### 1-2. PointHistory — 불변 필드 + Point VO

**파일**: `domain/point/model/PointHistory.kt`

- 모든 `var ... private set` → `val` (refUserPointId, type, amount, refOrderId)
- `amount: Long` → `amount: Point` 타입 변경
- init 블록: `if (amount.value <= 0)` 으로 검증 유지 (Point는 >= 0 허용이므로 > 0 체크 필요)

**파일**: `infrastructure/point/PointHistoryEntity.kt`
- `fromDomain`: `amount = pointHistory.amount.value`
- `toDomain`: `amount = Point(amount)`

**파일**: `domain/point/PointCharger.kt` — PointHistory 생성부 `amount = Point(amount)` 로 변경
**파일**: `domain/point/PointPaymentProcessor.kt` — PointHistory 생성부 동일 변경

**테스트**: `PointHistoryTest.kt` — Point VO 생성자 사용하도록 수정

### 1-3. UserPoint — guard() 인라인 + Point 파라미터

**파일**: `domain/point/model/UserPoint.kt`

- `guard()` 메서드 삭제, `init { Point(balance) }` 로 인라인
- `fun charge(amount: Long)` → `fun charge(amount: Point)`
    - 내부: `amount.value <= 0` 검증 제거 (Point가 >= 0 보장, 0 체크만 추가)
    - `Point(balance).plus(amount).value` 로 계산
- `fun use(amount: Long)` → `fun use(amount: Point)`
    - 동일 패턴으로 변경
- `fun canAfford(amount: Long)` → `fun canAfford(amount: Point)`

**테스트**: `UserPointTest.kt` — 모든 charge/use/canAfford 호출을 `Point(값)` 으로 변경

### 1-4. PointCharger — Point 파라미터 + 이중 검증 제거

**파일**: `domain/point/PointCharger.kt`

- `fun charge(userId: Long, amount: Long)` → `fun charge(userId: Long, amount: Point)`
- `amount <= 0` 검증 삭제 (Point가 >= 0 보장)
- `amount.value == 0L` 검증 추가 (0 포인트 충전 방지)
- `amount.value > MAX_CHARGE_AMOUNT` 검증 유지
- `userPoint.charge(amount)` — Point 직접 전달
- PointHistory 생성: `amount = amount` (이미 Point 타입)

**테스트**: `PointChargerTest.kt` — Point VO 사용하도록 수정

### 1-5. PointPaymentProcessor → PointPayer 리네이밍 + Money→Point 변환

**파일**: `domain/point/PointPaymentProcessor.kt` → `domain/point/PointPayer.kt`

- 클래스명: `PointPaymentProcessor` → `PointPayer`
- `fun usePoints(userId: Long, amount: Money, refOrderId: Long)` 시그니처 유지
- 내부: `val pointAmount = Point(amount.toLong())` — Money→Point 명시적 변환
- `userPoint.use(pointAmount)` — Point 직접 전달
- PointHistory 생성: `amount = pointAmount`

**파일**: `application/order/PlaceOrderUseCase.kt` — import + 필드명 변경
**파일**: `test/application/order/PlaceOrderUseCaseTest.kt` — import + 필드명 변경
**파일**: `test/domain/point/PointPaymentProcessorTest.kt` → 리네이밍 (존재하면)

**검증**: `./gradlew ktlintFormat && ./gradlew test`

---

## CP2: Order Aggregate Root 리팩토링 (Structural)

Order가 OrderItem의 라이프사이클을 제어하도록 변경. Aggregate Root 패턴 강화.

### 2-1. OrderItem.create 시그니처 변경

**파일**: `domain/order/model/OrderItem.kt`

- `fun create(product: OrderProductInfo, quantity: Int, orderId: Long)` → `fun create(product: OrderProductInfo, quantity: Int)`
- `refOrderId` 초기값 0 (`refOrderId = 0`)
- `fun assignToOrder(orderId: Long)` 메서드 추가 — `this.refOrderId = orderId`

### 2-2. Order.create + items 필드

**파일**: `domain/order/model/Order.kt`

- 생성자에 `items: List<OrderItem> = emptyList()` 파라미터 추가
- `val items: List<OrderItem> = items` 필드 추가
- `create` 시그니처 변경:
  ```kotlin
  fun create(userId: Long, items: List<Pair<OrderProductInfo, Int>>): Order {
      val orderItems = items.map { (info, quantity) ->
          OrderItem.create(info, quantity)
      }
      val totalPrice = orderItems.fold(Money(BigDecimal.ZERO)) { acc, item ->
          acc + (item.productPrice * item.quantity)
      }
      return Order(
          refUserId = userId,
          status = OrderStatus.CREATED,
          totalPrice = totalPrice,
          items = orderItems,
      )
  }
  ```
- `fun assignOrderIdToItems(orderId: Long)` 메서드 추가:
  ```kotlin
  fun assignOrderIdToItems(orderId: Long) {
      items.forEach { it.assignToOrder(orderId) }
  }
  ```
- `fromPersistence`에 `items: List<OrderItem> = emptyList()` 파라미터 추가

### 2-3. OrderEntity 매핑 — items 무시

**파일**: `infrastructure/order/OrderEntity.kt`

- `fromDomain`: 변경 없음 (items는 JPA 관계 아니므로 무시)
- `toDomain`: `Order.fromPersistence(... items = emptyList())` — 명시적

### 2-4. PlaceOrderUseCase 간소화

**파일**: `application/order/PlaceOrderUseCase.kt`

```kotlin
@Transactional
fun execute(userId: Long, command: PlaceOrderCommand): OrderInfo {
    if (command.items.isEmpty()) throw CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있습니다.")

    val products = productRepository.findAllByIdsForUpdate(command.items.map { it.productId })
    if (products.size != command.items.size) {
        throw CoreException(ErrorType.BAD_REQUEST, "존재하지 않는 상품이 포함되어 있습니다.")
    }
    val productMap = products.associateBy { it.id }

    val orderItemInputs = command.items.map { item ->
        val product = productMap[item.productId]
            ?: throw CoreException(ErrorType.BAD_REQUEST, "존재하지 않는 상품이 포함되어 있습니다.")
        if (!product.isAvailableForOrder()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 불가 상품이 포함되어 있습니다.")
        }
        product.decreaseStock(item.quantity)
        OrderProductInfo(product.id, product.name, product.price) to item.quantity
    }
    productRepository.saveAll(products)

    val order = Order.create(userId, orderItemInputs)
    val savedOrder = orderRepository.save(order)

    order.assignOrderIdToItems(savedOrder.id)
    val savedItems = orderItemRepository.saveAll(order.items)

    pointPayer.usePoints(userId, savedOrder.totalPrice, savedOrder.id)

    return OrderInfo.from(OrderDetail(savedOrder, savedItems))
}
```

- `!!` 연산자 완전 제거 (#12)
- 중간 변수 (quantityMap, foundIds, missingIds) 제거
- Aggregate Root를 통한 OrderItem 생성 — UseCase가 OrderItem을 직접 만지지 않음

### 2-5. 테스트 수정

**파일**: `test/domain/order/OrderTest.kt`
- `Order.create(userId, totalPrice)` → `Order.create(userId, items: List<Pair<OrderProductInfo, Int>>)` 로 변경
- items 기반 totalPrice 자동 계산 검증 테스트 추가

**파일**: `test/application/order/PlaceOrderUseCaseTest.kt`
- PointPaymentProcessor → PointPayer 임포트/필드명 변경
- UseCase 내부 흐름 변경에 맞춰 fixture 조정

**파일**: `test/domain/order/FakeOrderRepository.kt` — 변경 없을 가능성 높음 (items 미포함)

**검증**: `./gradlew ktlintFormat && ./gradlew test`

---

## CP3: 버그 수정 (Behavioral — TDD)

### 3-1. Like 동시성 — findByIdForUpdate

**[RED]** AddLikeUseCase 동시성 테스트 — 비관적 락 검증 (기존 테스트로 충분할 수 있음)

**[GREEN]**
- **파일**: `domain/catalog/product/repository/ProductRepository.kt` — `fun findByIdForUpdate(id: Long): Product?` 추가
- **파일**: `infrastructure/catalog/product/ProductRepositoryImpl.kt` — `@Lock(PESSIMISTIC_WRITE)` 구현
- **파일**: `application/like/AddLikeUseCase.kt` — `findById` → `findByIdForUpdate`
- **파일**: `application/like/RemoveLikeUseCase.kt` — `findById` → `findByIdForUpdate`

### 3-2. UpdateProductUseCase — valueOf 에러 처리

**[RED]** `test/application/catalog/product/UpdateProductUseCaseTest.kt`
- 잘못된 status 문자열 입력 시 BAD_REQUEST 에러 검증

**[GREEN]**
- **파일**: `application/catalog/product/UpdateProductUseCase.kt`
  ```kotlin
  val domainStatus = status?.let {
      try { Product.ProductStatus.valueOf(it) }
      catch (e: IllegalArgumentException) {
          throw CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 상품 상태입니다: $it")
      }
  }
  ```

### 3-3. RegisterUserUseCase — 동시 가입 방어

**[RED]** `test/application/user/RegisterUserUseCaseTest.kt`
- DataIntegrityViolationException 발생 시 CONFLICT 에러 변환 검증

**[GREEN]**
- **파일**: `application/user/RegisterUserUseCase.kt`
  ```kotlin
  val savedUser = try {
      userRepository.save(user)
  } catch (e: DataIntegrityViolationException) {
      throw CoreException(ErrorType.CONFLICT, "이미 존재하는 아이디입니다.")
  }
  ```

### 3-4. DeleteBrandUseCase — 이미 삭제된 브랜드 가드

**[RED]** `test/application/catalog/brand/DeleteBrandUseCaseTest.kt`
- 이미 삭제된 브랜드에 대해 products 재처리하지 않음 검증 (기존 테스트가 이미 커버할 수 있음)

**[GREEN]**
- **파일**: `application/catalog/brand/DeleteBrandUseCase.kt`
  ```kotlin
  fun execute(brandId: Long) {
      val brand = brandRepository.findById(brandId) ?: return
      if (brand.isDeleted()) return  // early return — 이미 삭제됨
      brand.delete()
      // ...
  }
  ```

**검증**: `./gradlew ktlintFormat && ./gradlew test`

---

## CP4: UseCase 분리 + 인프라 정리 (Structural)

### 4-1. GetBrandUseCase → GetBrandUseCase + GetBrandAdminUseCase 분리

**파일** (신규): `application/catalog/brand/GetBrandAdminUseCase.kt`
```kotlin
@Component
class GetBrandAdminUseCase(private val brandRepository: BrandRepository) {
    @Transactional(readOnly = true)
    fun execute(brandId: Long): BrandInfo {
        val brand = brandRepository.findById(brandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        return BrandInfo.from(brand)
    }
}
```

**파일**: `application/catalog/brand/GetBrandUseCase.kt`
- `executeAdmin` 메서드 삭제
- `executeActive` → `execute` 리네이밍

**파일**: `interfaces/api/brand/BrandV1Controller.kt`
- `getBrandUseCase.executeActive(brandId)` → `getBrandUseCase.execute(brandId)`

**파일**: `interfaces/api/brand/BrandAdminV1Controller.kt`
- `GetBrandUseCase` → `GetBrandAdminUseCase` 임포트/필드 변경
- `getBrandUseCase.executeAdmin(brandId)` → `getBrandAdminUseCase.execute(brandId)`

### 4-2. @Transactional(readOnly = true) 추가 (7개)

| 파일 | 메서드 |
|------|--------|
| `application/order/GetOrderUseCase.kt` | `execute` |
| `application/order/GetOrderAdminUseCase.kt` | `execute` |
| `application/order/GetOrdersUseCase.kt` | `execute` |
| `application/order/GetOrdersAdminUseCase.kt` | `execute` |
| `application/user/GetUserInfoUseCase.kt` | `execute` |
| `application/user/AuthenticateUserUseCase.kt` | `execute` |
| `application/point/GetUserPointUseCase.kt` | `execute` |

### 4-3. JpaRepository internal 키워드 추가 (8개)

| 파일 | 인터페이스 |
|------|-----------|
| `infrastructure/user/UserRepositoryImpl.kt` | `UserJpaRepository` |
| `infrastructure/order/OrderRepositoryImpl.kt` | `OrderJpaRepository` |
| `infrastructure/order/OrderItemRepositoryImpl.kt` | `OrderItemJpaRepository` |
| `infrastructure/like/LikeRepositoryImpl.kt` | `LikeJpaRepository` |
| `infrastructure/point/UserPointRepositoryImpl.kt` | `UserPointJpaRepository` |
| `infrastructure/point/PointHistoryRepositoryImpl.kt` | `PointHistoryJpaRepository` |
| `infrastructure/catalog/product/ProductRepositoryImpl.kt` | `ProductJpaRepository` |
| `infrastructure/catalog/brand/BrandRepositoryImpl.kt` | `BrandJpaRepository` |

### 4-4. LikeFacadeTest → LikeUseCaseTest 리네이밍

**파일**: `test/application/like/LikeFacadeTest.kt` → `test/application/like/LikeUseCaseTest.kt`
- 파일명 + 클래스명 `LikeFacadeTest` → `LikeUseCaseTest`

**검증**: `./gradlew ktlintFormat && ./gradlew test`

---

## CP5: 테스트 추가

### 5-1. ChargePointUseCaseTest

**파일** (신규): `test/application/point/ChargePointUseCaseTest.kt`

- Fake: FakeUserPointRepository, FakePointHistoryRepository
- PointCharger + ChargePointUseCase 조립
- 테스트 케이스:
    - 정상 충전 시 PointBalanceInfo 반환 검증
    - 0 포인트 충전 시 BAD_REQUEST
    - MAX_CHARGE_AMOUNT 초과 시 BAD_REQUEST
    - 존재하지 않는 userId 시 NOT_FOUND

**검증**: `./gradlew ktlintFormat && ./gradlew test`

---

## CP6: 문서 업데이트

### 6-1. round3-decisions.md — CQRS 검토 항목 추가

`docs/note/round3-decisions.md`에 신규 섹션 추가:
- GetOrdersUseCase/GetOrdersAdminUseCase의 `findItemsByOrders` 중복 코드
- CQRS 패턴 도입 시 해소 가능
- 이번 라운드에서는 결정 보류, 다음 라운드에서 재검토

### 6-2. CLAUDE.md 동기화

**업데이트 대상**:
- `apps/commerce-api/CLAUDE.md`: AuthenticateUserUseCase → UseCase 경유 (AuthService 언급 제거)
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/CLAUDE.md`:
    - Domain Service 테이블: `PointPaymentProcessor` → `PointPayer`
    - Order Aggregate Root 섹션: Order.create가 items 생성 + totalPrice 계산 패턴 반영
    - OrderItem.create 시그니처 변경 반영
- `apps/commerce-api/src/main/kotlin/com/loopers/application/CLAUDE.md`:
    - UseCase 예시 코드: PlaceOrderUseCase 최신 패턴 반영
    - `PlaceOrderCommand` 네이밍 확인 (PlaceOrder 유지)
- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/CLAUDE.md`:
    - JpaRepository `internal` 키워드 명시
    - Aggregate 저장 규칙: `order.assignOrderIdToItems(savedOrder.id)` 패턴 반영

---

## 검증

각 CP 완료 후:
```bash
./gradlew ktlintFormat
./gradlew ktlintCheck
./gradlew test
```

전체 완료 후:
```bash
./gradlew ktlintCheck test
```

## 변경 파일 목록 (예상)

### Domain (10)
- `domain/like/model/Like.kt`
- `domain/point/model/PointHistory.kt`
- `domain/point/model/UserPoint.kt`
- `domain/point/vo/Point.kt` (변경 없을 수 있음)
- `domain/point/PointCharger.kt`
- `domain/point/PointPaymentProcessor.kt` → `domain/point/PointPayer.kt`
- `domain/order/model/Order.kt`
- `domain/order/model/OrderItem.kt`
- `domain/order/OrderProductInfo.kt` (변경 없음)
- `domain/catalog/product/repository/ProductRepository.kt`

### Infrastructure (10)
- `infrastructure/point/PointHistoryEntity.kt`
- `infrastructure/catalog/product/ProductRepositoryImpl.kt`
- `infrastructure/user/UserRepositoryImpl.kt`
- `infrastructure/order/OrderRepositoryImpl.kt`
- `infrastructure/order/OrderItemRepositoryImpl.kt`
- `infrastructure/order/OrderEntity.kt`
- `infrastructure/like/LikeRepositoryImpl.kt`
- `infrastructure/point/UserPointRepositoryImpl.kt`
- `infrastructure/point/PointHistoryRepositoryImpl.kt`
- `infrastructure/catalog/brand/BrandRepositoryImpl.kt`

### Application (12)
- `application/order/PlaceOrderUseCase.kt`
- `application/like/AddLikeUseCase.kt`
- `application/like/RemoveLikeUseCase.kt`
- `application/catalog/product/UpdateProductUseCase.kt`
- `application/catalog/brand/GetBrandUseCase.kt`
- `application/catalog/brand/GetBrandAdminUseCase.kt` (신규)
- `application/catalog/brand/DeleteBrandUseCase.kt`
- `application/user/RegisterUserUseCase.kt`
- `application/order/GetOrder{s}{Admin}UseCase.kt` (4개 — readOnly)
- `application/user/GetUserInfoUseCase.kt`, `AuthenticateUserUseCase.kt`
- `application/point/GetUserPointUseCase.kt`

### Interfaces (2)
- `interfaces/api/brand/BrandV1Controller.kt`
- `interfaces/api/brand/BrandAdminV1Controller.kt`

### Tests (~8)
- `test/domain/order/OrderTest.kt`
- `test/domain/point/UserPointTest.kt`
- `test/domain/point/PointHistoryTest.kt`
- `test/domain/point/PointChargerTest.kt`
- `test/application/order/PlaceOrderUseCaseTest.kt`
- `test/application/like/LikeFacadeTest.kt` → `LikeUseCaseTest.kt`
- `test/application/point/ChargePointUseCaseTest.kt` (신규)
- `test/application/user/RegisterUserUseCaseTest.kt`

### Docs (4+)
- `docs/note/round3-decisions.md`
- `apps/commerce-api/CLAUDE.md`
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/CLAUDE.md`
- `apps/commerce-api/src/main/kotlin/com/loopers/application/CLAUDE.md`
- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/CLAUDE.md`
