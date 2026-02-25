# Application 레이어

Controller와 Domain 사이의 유일한 진입점. 모든 Controller는 반드시 UseCase를 통과한다.

## UseCase

```kotlin
@Component
class CreateOrderUseCase(
    private val productRepository: ProductRepository,
    private val userPointRepository: UserPointRepository,
    private val orderRepository: OrderRepository
) {
    @Transactional
    fun execute(command: CreateOrderCommand): OrderInfo {
        // 1. 객체 로드
        val product = productRepository.findById(command.productId)
        val userPoint = userPointRepository.findByUserId(command.userId)

        // 2. 도메인 규칙 실행 (각 객체의 상태를 변경하는 건 도메인 객체 스스로가 함)
        product.deductStock(command.quantity) // 내부에 "재고가 0보다 작아지면 예외" 로직 존재
        userPoint.usePoint(product.price * command.quantity) // 내부에 "잔고 부족 예외" 로직 존재

        // 3. 주문 객체 생성
        val order = Order.create(...)

        // 4. 저장
        productRepository.save(product)
        userPointRepository.save(userPoint)
        val savedOrder = orderRepository.save(order)

        return OrderInfo.from(savedOrder)
    }
}
```

- `@Component` 등록, 단일 책임 (`execute` 메서드)
- `@Transactional` 경계 설정
- 비즈니스 로직 없음. Domain Layer의 컴포넌트(Entity, Repository, Domain Service)를 용도에 맞게 오케스트레이션한다.
- **기본 패턴**: UseCase가 Repository를 직접 호출한다. 의미 없는 빈 껍데기 Domain Service를 만들지 않는다.
- **Domain Service 사용 시점**: 여러 Entity 간 원자적 얽힘이 있거나 복잡한 정책 검증이 필요한 경우에만 Domain Service에 위임한다 (예: `PointDeductor` — 잔고 차감 +
  이력 생성 원자성 보장).
- 계층 건너뛰기 방지: 단일 도메인의 단순 조회(Repository 호출)라도 Controller가 직접 부르지 않고 반드시 UseCase를 거친다 (Strict Layered Architecture).

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

## UseCase 네이밍 규칙

대고객 UseCase와 어드민 UseCase는 접미사로 구분한다.

| 대상   | 네이밍 패턴          | 예시                                               |
|------|-----------------|--------------------------------------------------|
| 대고객  | `XxxUseCase`    | `GetProductUseCase`, `GetOrderUseCase`           |
| 어드민  | `XxxAdminUseCase` | `GetProductAdminUseCase`, `GetOrderAdminUseCase` |

동일한 도메인 객체를 조회하더라도 대고객/어드민 권한 경계가 다르므로 UseCase를 분리한다.

- 대고객 UseCase: 활성 데이터만 반환, 삭제/HIDDEN 필터링 적용
- 어드민 UseCase: 모든 상태 포함, 삭제된 엔티티도 조회 가능

## @Transactional 위치

`@Transactional`은 UseCase의 `execute()` 메서드에 부착한다.

```kotlin
@Component
class CreateOrderUseCase(...) {
    @Transactional           // UseCase execute()에 부착
    fun execute(command: CreateOrderCommand): OrderInfo { ... }
}
```

**금지:** Domain Service(`CatalogService`, `UserPointService` 등)에 `@Transactional`을 사용하지 않는다. 트랜잭션 경계는 반드시 UseCase(Application 계층)에서 관리한다.

## Application Command

여러 도메인에 걸친 오케스트레이션이 필요한 경우 Application 전용 Command를 application 레이어에 정의한다 (예: `PlaceOrderCommand`, `CatalogCommand`). 단일
도메인이면 Domain Command를 UseCase 내부에서 직접 생성하거나, Domain 레이어의 Command를 그대로 사용한다.
