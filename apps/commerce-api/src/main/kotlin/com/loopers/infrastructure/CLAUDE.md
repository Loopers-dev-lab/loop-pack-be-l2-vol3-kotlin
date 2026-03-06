# Infrastructure 레이어

Domain Model과 DB 사이의 변환을 전담한다.

## JPA Entity (Persistence Model)

DB 테이블 매핑 전용. 비즈니스 로직 없음. `BaseEntity` 상속 (id, createdAt, updatedAt, deletedAt 자동관리, soft delete 지원).

**매핑 메서드:**

```kotlin
@Entity
@Table(name = "products")
class ProductEntity(
    @Column(nullable = false) val name: String,
    @Column(nullable = false) val price: BigDecimal,   // VO → 원시 타입
    // ...
) : BaseEntity() {
    companion object {
        fun fromDomain(product: Product): ProductEntity {
            return ProductEntity(
                name = product.name,
                price = product.price.value,       // Money.value
            ).withBaseFields(id = product.id)       // BaseEntity 필드 일괄 설정
        }
    }

    fun toDomain(): Product = Product.fromPersistence(
        id = id, name = name, price = Money(price), // ...
    )
}
```

- `fromDomain()`: Domain → Entity. VO는 `.value`로 원시 타입 추출. `BaseEntity` 필드(id, createdAt 등)는 `withBaseFields()` 유틸리티로 설정.
- `toDomain()`: Entity → Domain. 두 가지 패턴이 공존한다:
  - **`fromPersistence()` 팩토리**: `private constructor` 모델 (Order, OrderItem 등). 검증 건너뜀.
  - **`public constructor` 직접 호출**: Like 등. 생성자에 `id`를 포함하여 Reflection 없이 매핑.
- 확장함수(`toDomain()`)는 사용하지 않는다.
- DB 조회가 필요한 경우에만 예외적으로 `@Component` Mapper 도입.

## Repository 구현

`XxxRepositoryImpl.kt` 파일 **하나**에 JpaRepository 인터페이스와 구현체를 함께 선언한다.

```kotlin
interface ProductJpaRepository : JpaRepository<ProductEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): ProductEntity?
}

@Repository
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
) : ProductRepository {
    override fun findById(id: Long): Product? =
        productJpaRepository.findByIdAndDeletedAtIsNull(id)?.toDomain()
    override fun save(product: Product): Product =
        productJpaRepository.save(ProductEntity.fromDomain(product)).toDomain()
}
```

- 별도 `XxxJpaRepository.kt` 파일 생성 금지.
- JpaRepository 변수명은 `xxxJpaRepository` 풀네임으로 작성한다.

## Soft Delete 필터링 패턴

적용 대상: Brand, Product, Order, User

**단건 조회 (findById 등):** `deletedAt` DB 필터링 없음. 삭제된 레코드도 반환한다. UseCase에서 `isDeleted()`로 검증한다.

**다건 조회 (페이징 등):** `deletedAt IS NULL` 필터링 유지. DB 레벨에서 삭제된 레코드를 제외한다.

```kotlin
// JpaRepository
interface ProductJpaRepository : JpaRepository<ProductEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): ProductEntity?    // 다건 내부용 (삭제 제외)
    // 단건 findById → JpaRepository 기본 제공 (삭제 포함)
}

// Repository 구현
override fun findById(id: Long): Product? =
    productJpaRepository.findById(id).orElse(null)?.toDomain()  // deletedAt 필터 없음

override fun findAll(page: Int, size: Int): PageResult<Product> =
    // deletedAt IS NULL 필터링 적용 (다건)
```

**findByIdIncludeDeleted 불필요:** `findById`가 이미 삭제된 레코드를 포함하여 반환하므로 별도 메서드를 선언하지 않는다.

**findByIdForUpdate:** 비관적 락(`@Lock(LockModeType.PESSIMISTIC_WRITE)`)을 적용한다. likeCount 증가/감소 등 동시성 제어가 필요한 경우에 사용한다.

```kotlin
// 비관적 락 (deletedAt 필터 없음)
@Lock(LockModeType.PESSIMISTIC_WRITE)
fun findByIdWithLock(id: Long): Optional<ProductEntity>
// Spring Data JPA 메서드명 쿼리로 표현할 수 없으므로 별도 메서드명으로 선언하고
// @Lock 어노테이션으로 FOR UPDATE를 적용한다.
```

## JPA 연관관계 전면 미사용

`@OneToMany`, `@ManyToOne`, `@ManyToMany` 등을 사용하지 않는다.

- N+1 문제, Lazy/Eager 전략, `fetch join` 등의 복잡도를 원천 차단.
- 연관 데이터는 각 Repository를 통해 명시적으로 조회.

## Aggregate 저장 규칙

JPA 연관관계 미사용이므로 Aggregate Root 저장 시 Service 계층에서 내부 컬렉션을 명시적으로 순회하며 개별 Repository를 직접 호출한다.

```kotlin
val savedOrder = orderRepository.save(order)
order.assignOrderIdToItems(savedOrder.id)
orderItemRepository.saveAll(order.items)
```

## 페이지네이션 정렬 규칙

- 페이지네이션 쿼리에는 **반드시 안정적인 정렬 기준을 명시**한다
- 정렬 기준 없으면 동시 생성/삭제 시 같은 데이터가 여러 페이지에 나타나거나 누락된다
- 기본 정렬: `Sort.by(Sort.Direction.DESC, "id")`
- 비즈니스 정렬이 필요한 경우에도 마지막에 `id`를 추가하여 안정성 보장

```kotlin
// 올바른 패턴
val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))

// 잘못된 패턴: 정렬 기준 없음
val pageable = PageRequest.of(page, size)
```
