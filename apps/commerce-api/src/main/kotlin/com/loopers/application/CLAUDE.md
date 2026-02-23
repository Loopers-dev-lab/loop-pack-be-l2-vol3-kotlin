# Application 레이어

Cross-domain 유스케이스 오케스트레이션. 비즈니스 로직은 Domain에 위임하고 흐름만 조율한다.

## Facade

```kotlin
@Service
class OrderFacade(
    private val catalogService: CatalogService,
    private val orderService: OrderService,
    private val userPointService: UserPointService,
) {
    @Transactional
    fun createOrder(command: OrderCommand.CreateOrder): OrderInfo {
        val products = catalogService.getOrderableProducts(command.productIds)
        val order = orderService.createOrder(command, products)
        userPointService.use(command.userId, order.totalPrice)
        return OrderInfo.from(order)
    }
}
```

- `@Transactional` 경계 설정
- Domain Service 오케스트레이션
- 비즈니스 로직 없음. 흐름 조율만.

**Facade 생략 조건:** 단일 Domain Service로 충분한 경우 Controller → Domain Service 직접 호출.

## DTO 변환 규칙

- **Controller → Domain Service 직접 호출**: Domain Service가 Domain Model 반환 → Controller에서 Dto로 변환
- **같은 BC 내 조합** (예: Product + Brand): 조합 결과물은 **domain 레이어**에 데이터 클래스로 둔다 (예: `ProductDetail`). Application에 두면 DIP 위반.
- **다른 BC 간 조합** (Facade 경유): Facade가 **application 레이어의 Info 객체**를 반환 → Controller에서 Dto로 변환
- Dto/Info에 `companion object { fun from(...) }` 팩토리 메서드 사용
