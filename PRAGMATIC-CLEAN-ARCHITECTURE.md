# Pragmatic Clean Architecture

이 프로젝트의 핵심 아키텍처 철학. 코드 작성/리팩토링 시 1순위로 적용한다.

---

## 1. Domain Model ↔ JPA Entity 완전 분리

### Domain Model (`domain/` 레이어)
- **순수 POJO**. JPA 애노테이션(`@Entity`, `@Column` 등) 없음.
- 비즈니스 규칙, 상태 변경, 상태 판단을 **스스로** 수행하는 **Rich Domain Model**.
- `Order`는 `val items: List<OrderItem>`을 보유하고 totalPrice를 자체 계산한다.

### JPA Entity — Persistence Model (`infrastructure/` 레이어)
- DB 테이블 매핑 **전용**. 비즈니스 로직 없음.
- 매핑 메서드를 보유한다:
  ```kotlin
  @Entity
  class ProductEntity(...) {
      companion object {
          fun fromDomain(product: Product): ProductEntity = ProductEntity(...)
      }

      fun toDomain(): Product = Product(...)
  }
  ```

---

## 2. 매핑 전략 (Boilerplate 최소화)

- Entity의 `companion object { fun fromDomain() }` + `fun toDomain()` 방식 사용.
- 확장함수(`toDomain()`)는 사용하지 않는다.
- DB 조회가 필요한 경우에만 예외적으로 `@Component` Mapper 도입.

---

## 3. Repository 구조 — 파일 하나로

`XxxRepositoryImpl.kt` 파일 하나에 JpaRepository 인터페이스와 구현체를 함께 선언한다.

```kotlin
// infrastructure/product/ProductRepositoryImpl.kt

internal interface ProductJpaRepository : JpaRepository<ProductEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): ProductEntity?
}

@Repository
class ProductRepositoryImpl(
    private val jpa: ProductJpaRepository,
) : ProductRepository {

    override fun findById(id: Long): Product? =
        jpa.findByIdAndDeletedAtIsNull(id)?.toDomain()

    override fun save(product: Product): Product =
        jpa.save(ProductEntity.fromDomain(product)).toDomain()
}
```

- `XxxJpaRepository`는 구현 세부사항 → `internal`로 외부 노출 차단.
- 별도 `XxxJpaRepository.kt` 파일 생성 금지.

---

## 4. Domain Service — Spring 의존성 타협 허용

```kotlin
@Service  // 또는 @Component — 둘 다 허용
class CatalogService(private val productRepository: ProductRepository) {
    fun findProduct(id: Long): Product = ...
}
```

- Domain Entity(Model)은 절대적 순수성 유지.
- Domain Service는 `@Service`/`@Component` 허용 (Spring DI 타협).

---

## 5. Application Layer — Thin Facade

```kotlin
@Service
class OrderFacade(
    private val catalogService: CatalogService,
    private val orderService: OrderService,
    private val userPointService: UserPointService,
) {
    @Transactional
    fun createOrder(command: OrderCommand.CreateOrder): OrderInfo {
        // 비즈니스 로직 없음. 흐름 조율만.
        val products = catalogService.getOrderableProducts(command.productIds)
        val order = orderService.createOrder(command, products)
        userPointService.use(command.userId, order.totalPrice)
        return OrderInfo.from(order)
    }
}
```

- `@Transactional` 경계 설정
- Domain Service 오케스트레이션
- 응답 DTO/Info 변환

---

## 6. Value Object — @JvmInline value class 적극 활용

Domain Model이 순수 POJO이므로 `@Converter` 부담이 사라진다. 모든 도메인 값을 VO로 표현할 수 있다.

```kotlin
// 단일 값 감싸기 → @JvmInline value class (런타임 오버헤드 없음)
@JvmInline value class Price(val value: BigDecimal) {
    init { require(value >= BigDecimal.ZERO) { "가격은 0 이상이어야 합니다" } }
}

@JvmInline value class Email(val value: String) {
    init { require(value.contains("@")) { "올바른 이메일 형식이 아닙니다" } }
}

// 도메인 메서드가 있는 VO → 일반 class
class Stock(val value: Int) {
    fun decrease(quantity: Int): Stock = Stock(value - quantity)
    fun isZero(): Boolean = value == 0
}

// 복합 필드 VO → data class
data class Address(val city: String, val street: String)
```

- 한 줄짜리 검증(`Price`, `Email`, `BrandName` 등)도 VO로 적극 표현한다.
- JPA Entity는 DB 컬럼 타입(String, BigDecimal 등)으로 저장하고, `toDomain()`에서 VO로 복원한다.

---

## 7. JPA 연관관계 전면 미사용

`@OneToMany`, `@ManyToOne`, `@ManyToMany` 등 JPA 연관관계 매핑을 사용하지 않는다.

- N+1 문제, Lazy/Eager 로딩 전략, `fetch join` 등의 복잡도를 원천 차단한다.
- 연관된 데이터는 각 Repository를 통해 명시적으로 조회한다.
- Aggregate 내부의 관계(예: `Order` → `OrderItem`)는 Domain Model 레벨에서 `List<OrderItem>`으로 표현하되, 영속성은 `OrderRepository` + `OrderItemRepository` 각각이 담당한다.
- Aggregate Root(`Order`)를 통한 상태 변경 컨벤션은 도메인 레이어 컨벤션으로 유지한다.

---

## 레이어 요약

```
interfaces/    → Controller, Dto
application/   → Facade (@Transactional + 오케스트레이션 + DTO 변환)
domain/        → Domain Model(순수 POJO), Domain Service, Repository(인터페이스), VO
infrastructure/→ XxxEntity(JPA), XxxRepositoryImpl(internal JpaRepository 포함)
```

**의존 방향:** `Application → Domain ← Infrastructure` (DIP)
