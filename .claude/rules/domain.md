---
paths:
  - "apps/commerce-api/src/main/kotlin/**/domain/**/*.kt"
---

# Domain Layer 규칙

## Entity

- JPA 어노테이션 + 비즈니스 로직을 함께 가진다 (분리하지 않음)
- `private constructor` + `companion object { fun create() }`
- 생성 시 유효성 검증
- 불변 필드는 `val`, 가변 필드는 `var ... protected set`
- 다른 엔티티를 직접 참조하지 않고 **ID(Long)만 보유** (ID 참조 원칙)
- `@OneToMany`, `@ManyToOne` 사용하지 않음 — N+1, cascade 사이드이펙트 방지

```kotlin
@Entity
class Product private constructor(
    brandId: Long,  // Brand 엔티티가 아닌 ID만
    name: String,
    price: Money,
    stock: Stock,
) : BaseEntity() {

    @Column(nullable = false)
    var brandId: Long = brandId
        protected set

    // 자기 데이터를 자기가 변경하는 비즈니스 행위
    fun decreaseStock(quantity: Int) {
        this.stock = stock.deduct(quantity)
    }

    companion object {
        fun create(brandId: Long, ...): Product {
            // 도메인 불변식 검증
            return Product(brandId, ...)
        }
    }
}
```

## 도메인 Entity 간 참조 규칙

Entity는 다른 도메인 Entity 타입을 직접 참조하지 않는다. (파라미터로 받는 것도 포함)

다른 도메인의 값이 필요한 경우:
- 단순한 값 (ID, 이름 등 스냅샷) → 원시 타입으로 전달

```kotlin
// ✅ 원시 타입으로 전달
class OrderItem private constructor(
    val productId: Long,        // Product 타입이 아닌 Long
    val productName: String,    // 스냅샷 값
    val brandName: String,      // 스냅샷 값
)

// ❌ 다른 도메인 Entity를 직접 참조
class OrderItem private constructor(
    val product: Product,       // 금지
    val brand: Brand,           // 금지
)
```

## Value Object (VO)

- 도메인 규칙을 값 자체에 내장 — 잘못된 값이 존재할 수 없음
- **불변** — 변경 시 새 인스턴스 반환 (원본은 그대로)
- 생성자에서 불변식 검증

```kotlin
@Embeddable
data class Money(val amount: Long) {
    init {
        require(amount >= 0) { "금액은 0 이상이어야 합니다" }
    }
}

@Embeddable
data class Stock(val quantity: Int) {
    init {
        require(quantity >= 0) { "재고는 0 이상이어야 합니다" }
    }

    fun deduct(amount: Int): Stock {
        if (quantity < amount) throw CoreException(ProductErrorCode.INSUFFICIENT_STOCK)
        return Stock(quantity - amount)
    }
}
```

**Entity vs VO 구분 기준:**
| 기준 | Entity | Value Object |
|------|--------|-------------|
| 식별자 | 있음 (id) | 없음 (값으로 비교) |
| 생명주기 | 독립적 | 소속 Entity에 종속 |
| 변경 | 필드 수정 | 새 인스턴스 생성 |

## Domain Service

- **상태 없음(stateless)** — 필드가 없고, 매번 데이터를 받아서 계산/판단만
- **Repository 주입 금지** — UseCase에서 데이터를 조회해서 넘겨줌 (순수 함수)
- 여러 Entity/VO 데이터를 받아 검증/계산하는 비즈니스 로직 담당
- UseCase에서 순수 Domain만으로 처리하지 못하는 부분을 위임받는다

```kotlin
// ✅ Repository 없음 — UseCase에서 데이터를 전달받아 순수 검증만 수행
@Component
class OrderValidator {
    fun validate(items: List<OrderLine>, products: List<Product>) {
        // 상품 존재 여부, 판매 상태, 재고 충분 여부를 전수 조사
    }
}

// ❌ Domain Service가 Repository를 주입받으면 안 됨
class OrderValidator(private val productRepository: ProductRepository) { ... }
```

**Entity에 넣을지 Domain Service에 넣을지 판단:**
- 자기 데이터만 필요 → Entity 메서드 (예: `Product.decreaseStock()`)
- 여러 Entity/VO의 데이터 필요, DB 조회 없음 → Domain Service (예: `OrderValidator.validate()`)
- DB 조회가 필요 → UseCase에서 처리

## UseCase에 있으면 안 되는 것 — Domain으로 내릴 신호

```kotlin
// ❌ Entity 필드 직접 조작 → Entity 메서드로
brand.status = BrandStatus.DELETED
product.stock -= quantity

// ✅
brand.delete()
product.decreaseStock(quantity)
```

```kotlin
// ❌ 도메인 상태 판단 if문 → Entity 메서드로
if (product.status != ProductStatus.ACTIVE || product.deletedAt != null) { ... }

// ✅
product.validateAvailable()
```

UseCase는 **"누구한테 뭘 시킬지"만 알고, "어떻게 하는지"는 모르는 게 목표**다.

## Repository Interface

- Domain Layer에 위치
- 구현은 Infrastructure Layer
- **Spring Data 타입 노출 금지** (`Pageable`, `Page<T>`, `Sort`)
- 페이지네이션: `page: Int, size: Int` → `PageResult<T>`
- 조회 조건: 도메인 자체 타입 사용 (`ProductSearchCondition` 등)
- **메서드 네이밍은 비즈니스 언어** (인프라 구현 방식 노출 금지)

```kotlin
// ✅ 비즈니스 언어
interface BrandRepository {
    fun findActiveByIdOrNull(id: Long): Brand?
    fun findAllActive(): List<Brand>
    fun existsActiveByName(name: String): Boolean
    fun save(brand: Brand): Brand
}

// ❌ 인프라 구현 노출
interface BrandRepository {
    fun findAllByDeletedAtIsNull(): List<Brand>
}
```

## 예외

- `CoreException(ErrorCode)` 사용
- 인증 실패는 항상 동일한 에러 (정보 노출 방지)

## BaseEntity 상속 전략

- BaseEntity: `id`, `createdAt`, `updatedAt`만 제공
- `deletedAt`은 soft delete가 필요한 엔티티(Brand, Product)가 직접 선언
- Like, OrderItem은 BaseEntity 상속하지 않고 필요한 필드만 직접 정의
