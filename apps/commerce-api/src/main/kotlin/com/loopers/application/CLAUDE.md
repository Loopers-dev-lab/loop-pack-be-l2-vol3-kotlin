# Application 레이어

Controller와 Domain 사이의 유일한 진입점. 모든 Controller는 반드시 UseCase를 통과한다.

## UseCase

```kotlin
@Component
class PlaceOrderUseCase(
    private val catalogService: CatalogService,
    private val orderService: OrderService,
    private val userPointService: UserPointService,
) {
    @Transactional
    fun execute(command: PlaceOrderCommand): OrderInfo {
        val products = catalogService.getOrderableProducts(command.productIds)
        val order = orderService.createOrder(command.toOrderCommand(), products)
        userPointService.use(command.userId, order.totalPrice)
        return OrderInfo.from(order)
    }
}
```

- `@Component` 등록, 단일 책임 (`execute` 메서드)
- `@Transactional` 경계 설정
- 비즈니스 로직 없음. Domain Service 오케스트레이션만.
- 단일 Domain Service 래핑이라도 UseCase를 생략하지 않는다 (Strict Layered Architecture)

## Info DTO (Application → Interfaces)

UseCase가 반환하는 Application 전용 DTO. **원시 타입만** 사용하여 Domain 객체(VO, Enum)가 Interfaces로 유출되지 않게 한다.

```kotlin
data class ProductInfo(
    val id: Long,
    val name: String,
    val price: BigDecimal,
    val status: String,      // Domain Enum → String 변환
) {
    companion object {
        fun from(product: Product): ProductInfo = ProductInfo(
            id = product.id,
            name = product.name,
            price = product.price.value,
            status = product.status.name,
        )
    }
}
```

- `companion object { fun from(domainModel) }` 팩토리 메서드
- Domain Enum은 `.name`으로 String 변환
- VO는 `.value`로 원시 타입 추출

## Application Command

cross-domain 오케스트레이션이 필요한 경우 Application 전용 Command 정의 (예: `PlaceOrderCommand`). 단일 도메인이면 Domain Command를 UseCase 내부에서 직접 생성.
