---
paths:
  - "apps/commerce-api/src/main/kotlin/**/domain/**/*.kt"
---

# Domain Layer 규칙

## Entity

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

## Value Object (VO)

- 도메인 규칙을 값 자체에 내장 — 잘못된 값이 존재할 수 없음
- **불변** — 변경 시 새 인스턴스 반환 (원본은 그대로)
- 생성자에서 불변식 검증

```kotlin
// Money: 금액 >= 0 보장
@Embeddable
data class Money(val amount: Long) {
    init {
        require(amount >= 0) { "금액은 0 이상이어야 합니다" }
    }
}

// Stock: 수량 >= 0, 차감 시 새 인스턴스 반환
@Embeddable
data class Stock(val quantity: Int) {
    init {
        require(quantity >= 0) { "재고는 0 이상이어야 합니다" }
    }

    fun deduct(amount: Int): Stock {
        // 부족하면 예외, 충분하면 새 Stock 반환
        if (quantity < amount) throw ProductException.insufficientStock(...)
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
- 하나의 Entity에 넣기 어려운 비즈니스 로직 담당
- 여러 도메인 객체의 협력이 필요할 때 사용

```kotlin
// 상태 없음: 필드 없이 계산만 수행
class OrderPriceCalculator {
    fun calculate(items: List<Pair<Product, Quantity>>): Money {
        val total = items.sumOf { (product, quantity) ->
            product.price.amount * quantity.value
        }
        return Money(total)
    }
}
```

**Entity에 넣을지 Domain Service에 넣을지 판단:**
- 자기 데이터만 필요 → Entity 메서드 (예: `Product.decreaseStock()`)
- 여러 Entity/VO의 데이터 필요 → Domain Service (예: 주문 금액 계산)

## Service

- `@Component` + `@Transactional`
- Repository 인터페이스만 의존 (구현체 X)
- DB 조회가 필요한 비즈니스 규칙 검증 (중복 체크 등)
- 비즈니스 규칙 자체는 도메인 메서드 호출로 위임

```kotlin
@Component
class ProductService(
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun decreaseStock(productId: Long, quantity: Int) {
        val product = productRepository.getById(productId)
        product.decreaseStock(quantity)  // 도메인에 위임
    }
}
```

## Repository Interface

- Domain Layer에 위치
- 구현은 Infrastructure Layer

```kotlin
interface ProductRepository {
    fun getById(id: Long): Product        // 없으면 예외
    fun findById(id: Long): Product?      // 없으면 null
    fun save(product: Product): Product
}
```

## 예외

- `{도메인}Exception.xxx()` 팩토리 메서드 사용
- 인증 실패는 항상 `invalidCredentials()` (정보 노출 방지)

## BaseEntity 상속 전략

- BaseEntity: `id`, `createdAt`, `updatedAt`만 제공
- `deletedAt`은 soft delete가 필요한 엔티티(Brand, Product)가 직접 선언
- Like, OrderItem은 BaseEntity 상속하지 않고 필요한 필드만 직접 정의
