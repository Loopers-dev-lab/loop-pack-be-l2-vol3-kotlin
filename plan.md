# Primitive Obsession 해소 + Brand 상태 검증 추가

## Context

리뷰 피드백 #3(Primitive Obsession), #4(Brand isDeleted 검증 누락) 반영.
- 모든 식별자가 `Long`이라 파라미터 순서 오류를 컴파일러가 잡지 못함
- Domain Model이 순수 POJO로 분리된 지금, JPA 제약 없이 필드를 VO화 가능
- GetProductUseCase에서 Brand 조회 후 삭제 여부 미검증

## 설계 결정

- **Id VO 범위**: 교차 참조 4개만 — `UserId`, `ProductId`, `OrderId`, `BrandId`
- **Controller/UseCase 경계**: UseCase가 Long 수용, 내부에서 VO 변환 → Controller 변경 없음
- **변환 경계**: UseCase(Long) → Domain/Repository(VO) → Infrastructure(Long↔VO 매핑)

---

## CP0: VO 생성 및 Money 이동 (non-breaking)

- [x] `domain/common/vo/UserId.kt` — `@JvmInline value class UserId(val value: Long)`
- [x] `domain/common/vo/ProductId.kt` — `@JvmInline value class ProductId(val value: Long)`
- [x] `domain/common/vo/OrderId.kt` — `@JvmInline value class OrderId(val value: Long)`
- [x] `domain/common/vo/BrandId.kt` — `@JvmInline value class BrandId(val value: Long)`
- [x] `domain/order/vo/Quantity.kt` — `@JvmInline value class Quantity(val value: Int)` + `init { require value > 0 }`
- [x] `domain/common/Money.kt` → `domain/common/vo/Money.kt` 이동 + import 전체 수정

**파일**: 신규 5개, 이동 1개 (Money)

---

## CP1: Domain 레이어 변경 ✅ 완료

### Domain Models

| 모델 | 변경 필드 |
|------|----------|
| `Product` | `id: Long → ProductId`, `refBrandId: Long → BrandId`, `stock: Int → Stock` |
| `Brand` | `id: Long → BrandId` |
| `User` | `id: Long → UserId` |
| `Order` | `id: Long → OrderId`, `refUserId: Long → UserId` |
| `OrderItem` | `refProductId: Long → ProductId`, `refOrderId: Long → OrderId`, `quantity: Int → Quantity` |
| `UserPoint` | `refUserId: Long → UserId`, `balance: Long → Point` |
| `PointHistory` | `refOrderId: Long? → OrderId?` |
| `Like` | `refUserId: Long → UserId`, `refProductId: Long → ProductId` |
| `OrderProductInfo` | `id: Long → ProductId` |

### Repository 인터페이스 시그니처 변경

| Repository | 변경 메서드 |
|-----------|-----------|
| `ProductRepository` | `findById(ProductId)`, `findByIdForUpdate(ProductId)`, `findActiveProducts(BrandId?)`, `findAllByBrandId(BrandId)`, `findAllByIds(List<ProductId>)`, `findAllByIdsForUpdate(List<ProductId>)` |
| `BrandRepository` | `findById(BrandId)` |
| `UserRepository` | `findById(UserId)` |
| `OrderRepository` | `findById(OrderId)`, `findAllByUserId(UserId, ...)` |
| `OrderItemRepository` | `findAllByOrderId(OrderId)`, `findAllByOrderIds(List<OrderId>)` |
| `LikeRepository` | `existsByUserIdAndProductId(UserId, ProductId)`, `findByUserIdAndProductId(UserId, ProductId)`, `findAllByUserId(UserId)` |
| `UserPointRepository` | `findByUserId(UserId)`, `findByUserIdForUpdate(UserId)` |
| `PointHistoryRepository` | `findAllByUserPointId(Long)` — UserPointId는 VO 범위 밖, Long 유지 |

### Domain Service

| Service | 변경 |
|---------|-----|
| `PointDeductor` | 내부에서 UserId 사용하는 부분 확인 |
| `PointCharger` | 동일 |

**파일**: ~20개 (모델 8 + 리포 8 + 서비스 2 + OrderProductInfo 1 + 기타)

---

## CP2: Infrastructure 레이어 변경 ✅ 완료

### Entity 매핑 (fromDomain / toDomain)

| Entity | 변경 |
|--------|-----|
| `ProductEntity` | `fromDomain`: `product.id.value`, `product.refBrandId.value`, `product.stock.value` / `toDomain`: `ProductId(id)`, `BrandId(refBrandId)`, `Stock(stock)` |
| `BrandEntity` | `fromDomain`: `brand.id.value` / `toDomain`: `BrandId(id)` |
| `UserEntity` | `fromDomain`: `user.id.value` / `toDomain`: `UserId(id)` |
| `OrderEntity` | `fromDomain`: `order.id.value`, `order.refUserId.value` / `toDomain`: `OrderId(id)`, `UserId(refUserId)` |
| `OrderItemEntity` | `fromDomain`: `item.refProductId.value`, `item.refOrderId.value`, `item.quantity.value` / `toDomain`: `ProductId(refProductId)`, `OrderId(refOrderId)`, `Quantity(quantity)` |
| `UserPointEntity` | `fromDomain`: `up.refUserId.value`, `up.balance.value` / `toDomain`: `UserId(refUserId)`, `Point(balance)` |
| `PointHistoryEntity` | `fromDomain`: `ph.refOrderId?.value` / `toDomain`: `refOrderId?.let { OrderId(it) }` |
| `LikeEntity` | `fromDomain`: `like.refUserId.value`, `like.refProductId.value` / `toDomain`: `UserId(refUserId)`, `ProductId(refProductId)` |

### RepositoryImpl 시그니처

Repository 인터페이스와 동일하게 VO 타입으로 변경. 내부에서 `.value`로 JPA 호출.

**파일**: ~16개 (Entity 8 + RepositoryImpl 8)

---

## CP3: Application 레이어 변경 ✅ 완료

### UseCase — execute() 시그니처는 Long 유지, 내부 변환

```kotlin
// 예시: GetProductUseCase
fun execute(productId: Long): CatalogInfo {
    val product = productRepository.findById(ProductId(productId))  // Long → VO 변환
    ...
}
```

모든 UseCase의 repository/domain service 호출 시 `Long → VO` 래핑 추가.

### Command — Long 유지

`PlaceOrderCommand.productId: Long`, `CatalogCommand.brandId: Long` 등 그대로.
UseCase 내부에서 `ProductId(command.productId)` 변환.

### Info DTO — `.value` 추출

```kotlin
// 예시: ProductInfo.from()
data class ProductInfo(val id: Long, ...) {
    companion object {
        fun from(product: Product) = ProductInfo(id = product.id.value, ...)
    }
}
```

**파일**: ~25개 (UseCase ~20 + Command 2 + Info 3-5)

---

## CP4: 테스트 변경 ✅ 완료

### Fake Repository (8개)

- 메서드 시그니처: Repository 인터페이스와 동일하게 VO 타입으로 변경
- 내부 비교: `it.id == id` → VO 간 비교 (value class 동등성으로 자동 동작)
- Reflection id 할당: `@JvmInline value class`는 JVM에서 Long으로 표현되므로 기존 reflection 코드 유지 가능
- 도메인 모델 생성: `Brand(id = BrandId(sequence++), ...)`

### TestFixture (2개)

- `ProductTestFixture.DEFAULT_BRAND_ID`: `1L → BrandId(1)`
- `ProductTestFixture.createProduct(refBrandId: BrandId)`

### Domain 테스트 (10개)

- Long 리터럴 → VO 래핑: `1L → UserId(1)`, `Order.create(UserId(1), ...)` 등
- `UserPoint(refUserId = UserId(1), balance = Point(0))` 등

### UseCase 테스트 (15+개)

- `useCase.execute()` 호출은 Long 유지 (시그니처 불변)
- 도메인 모델 생성 시 VO 사용: `Product(refBrandId = BrandId(1), ...)` 등
- Fake Repository 메서드 호출 시 VO 사용

### E2E 테스트 (4개)

- API 응답에서 id 추출은 Long (JSON 직렬화 시 value class → underlying type)
- DB 직접 저장 코드가 있으면 VO 사용

**파일**: ~35개

---

## CP5: Brand isDeleted 검증 (행위적 변경 — 별도 커밋)

Tidy First 원칙: 구조적 변경(CP0-4)과 행위적 변경(CP5)을 분리.

### [RED] 테스트 작성

`GetProductUseCaseTest`에 테스트 추가:
```
"삭제된 브랜드의 상품 조회 시 예외 발생"
```

### [GREEN] 구현

`GetProductUseCase.execute()`:
```kotlin
val brand = brandRepository.findById(BrandId(product.refBrandId.value))
    ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
if (brand.isDeleted()) {
    throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
}
```

**파일**: 2개 (UseCase 1 + Test 1)

---

## 검증

```bash
./gradlew :apps:commerce-api:ktlintCheck && ./gradlew :apps:commerce-api:test
```

## 변경 파일 총 수: ~80개 (CP0-4 구조 ~60, 테스트 ~35, CP5 행위 2)
