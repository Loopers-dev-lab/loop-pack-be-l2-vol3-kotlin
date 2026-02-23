# Infrastructure 레이어

Domain Model과 DB 사이의 변환을 전담한다.

## JPA Entity (Persistence Model)

DB 테이블 매핑 전용. 비즈니스 로직 없음. `BaseJpaEntity` 상속 (id, createdAt, updatedAt, deletedAt 자동관리, soft delete 지원).

**매핑 메서드:**

```kotlin
@Entity
class ProductEntity(...) {
    companion object {
        fun fromDomain(product: Product): ProductEntity = ProductEntity(...)
        // VO는 내부 기본 타입을 명시적으로 꺼내어 매핑: domain.price.value
    }
    fun toDomain(): Product = Product(...)
}
```

- 확장함수(`toDomain()`)는 사용하지 않는다.
- DB 조회가 필요한 경우에만 예외적으로 `@Component` Mapper 도입.

## Repository 구현

`XxxRepositoryImpl.kt` 파일 **하나**에 JpaRepository 인터페이스와 구현체를 함께 선언한다.

```kotlin
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

## JPA 연관관계 전면 미사용

`@OneToMany`, `@ManyToOne`, `@ManyToMany` 등을 사용하지 않는다.

- N+1 문제, Lazy/Eager 전략, `fetch join` 등의 복잡도를 원천 차단.
- 연관 데이터는 각 Repository를 통해 명시적으로 조회.

## Aggregate 저장 규칙

JPA 연관관계 미사용이므로 Aggregate Root 저장 시 Service 계층에서 내부 컬렉션을 명시적으로 순회하며 개별 Repository를 직접 호출한다.

```kotlin
orderRepository.save(order)
order.items.forEach { orderItemRepository.save(it) }
```
