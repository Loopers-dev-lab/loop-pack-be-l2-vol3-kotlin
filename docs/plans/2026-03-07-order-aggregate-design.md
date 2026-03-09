# Order Aggregate 개선 설계

**작성일**: 2026-03-07
**상태**: Approved (승인됨)
**목표**: Order Aggregate의 경계를 강화하고 OrderItem의 생명주기를 완전히 제어

---

## 1. 개선 배경

### 현재 문제점

1. **OrderItem이 BaseEntity 상속**
   - 독립적인 엔티티처럼 보임
   - 부모 Order 없이도 생성 가능

2. **OrderService에서 OrderItem 생성**
   ```kotlin
   val orderItem = OrderItem.create(
       orderId = savedOrder.id,  // ❌ 이미 저장된 Order의 ID로 수동 설정
       productId = itemRequest.productId,
       quantity = itemRequest.quantity,
       price = itemRequest.price,
   )
   savedOrder.addOrderItem(orderItem)  // 별도 추가
   ```
   - Order Root가 생성을 제어하지 않음
   - 부분 생성 상태 가능

3. **orderId 기본값 0L**
   ```kotlin
   class OrderItem protected constructor(
       val orderId: Long = 0L,  // ❌ 기본값
   )
   ```
   - Order 없이 생성될 수 있음

### 개선의 이점

- ✅ Aggregate 경계 명확화
- ✅ OrderItem은 Order 내에서만 생성 가능
- ✅ 부분 생성 상태 제거
- ✅ 데이터 일관성 보장
- ✅ 테스트 용이성 향상

---

## 2. 설계 결정

### 2.1 OrderItem 개선

**핵심 변경:**
- `orderId` 기본값 제거 → 필수 파라미터로 변경
- Order 인스턴스를 받아서 orderId 자동 설정
- 검증 로직 추가 (quantity, price)

```kotlin
@Entity
@Table(name = "order_items")
class OrderItem protected constructor(
    @Column(name = "order_id", nullable = false)
    val orderId: Long,  // 기본값 제거
    @Column(name = "product_id", nullable = false)
    val productId: Long,
    @Column(name = "product_name", nullable = false, length = 200)
    val productName: String,
    @Column(nullable = false)
    val quantity: Int,
    @Column(nullable = false, precision = 19, scale = 2)
    val price: BigDecimal,
) : BaseEntity() {

    @Column(nullable = false, precision = 19, scale = 2)
    var discountAmount: BigDecimal = BigDecimal.ZERO
        protected set

    fun getSubtotal(): BigDecimal {
        return (price * BigDecimal(quantity.toLong())) - discountAmount
    }

    fun applyDiscountAmount(discount: BigDecimal) {
        if (discount < BigDecimal.ZERO) {
            throw CoreException(ErrorType.BAD_REQUEST, "할인액은 0 이상이어야 합니다")
        }
        this.discountAmount = discount
    }

    fun getItemAmount(): BigDecimal {
        return price * BigDecimal(quantity.toLong())
    }

    companion object {
        fun create(
            order: Order,  // ✅ Order 인스턴스 전달
            product: Product,
            quantity: Int,
            price: BigDecimal,
        ): OrderItem {
            // ✅ 검증 추가
            require(quantity > 0) { "수량은 0보다 커야 합니다" }
            require(price > BigDecimal.ZERO) { "가격은 0보다 커야 합니다" }

            return OrderItem(
                orderId = order.id,  // ✅ Order에서 가져옴
                productId = product.id,
                productName = product.name,
                quantity = quantity,
                price = price,
            )
        }
    }
}
```

### 2.2 Order Aggregate 개선

**핵심 변경:**
1. `addOrderItem()` → `addItem()` (메서드 이름 단순화)
2. `addItem()` → `internal` (Order 내부에서만 호출)
3. `createWithItems()` Factory 메서드 추가 (Aggregate 동시 생성)

```kotlin
@Entity
@Table(name = "orders")
class Order protected constructor(
    val userId: Long = 0L,
    val couponId: Long? = null,
) : BaseEntity() {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OrderStatus = OrderStatus.PENDING
        protected set

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private val _orderItems: MutableList<OrderItem> = mutableListOf()

    val orderItems: List<OrderItem>
        get() = _orderItems.toList()

    // ✅ 내부용 메서드: Order 생성 후에만 호출 가능
    internal fun addItem(product: Product, quantity: Int, price: BigDecimal) {
        val item = OrderItem.create(this, product, quantity, price)
        _orderItems.add(item)
    }

    fun getOrderDate(): ZonedDateTime = createdAt

    fun getTotalPrice(): BigDecimal {
        return _orderItems.fold(BigDecimal.ZERO) { acc, item ->
            acc + item.getSubtotal()
        }
    }

    fun changeStatus(newStatus: OrderStatus) {
        this.status = newStatus
    }

    companion object {
        fun create(
            userId: Long,
            couponId: Long? = null,
            status: OrderStatus = OrderStatus.PENDING,
        ): Order = Order(userId = userId, couponId = couponId).apply {
            this.status = status
        }

        // ✅ 새 Factory: Aggregate를 함께 생성
        fun createWithItems(
            userId: Long,
            couponId: Long? = null,
            items: List<OrderItemSpec>,
        ): Order {
            require(items.isNotEmpty()) { "주문 항목은 최소 1개 이상이어야 합니다" }

            val order = Order(userId = userId, couponId = couponId)
            items.forEach { spec ->
                order.addItem(spec.product, spec.quantity, spec.price)
            }
            return order
        }
    }
}
```

### 2.3 OrderItemSpec DTO

새로운 순수 데이터 클래스 (OrderItem 생성용):

```kotlin
package com.loopers.domain.order.dto

import com.loopers.domain.product.Product
import java.math.BigDecimal

data class OrderItemSpec(
    val product: Product,
    val quantity: Int,
    val price: BigDecimal,
)
```

**용도:**
- OrderService → Order.createWithItems() 전달용
- Product 객체와 주문 수량/가격을 함께 전달

### 2.4 OrderService 개선

**변경점:**
1. `Order.createWithItems()` 사용
2. OrderItemSpec 준비
3. 검증 로직은 Order에서 처리

```kotlin
@Service
@Transactional(readOnly = true)
class OrderService(
    private val orderRepository: OrderRepository,
) {

    @Transactional
    fun createOrder(
        userId: Long,
        items: List<CreateOrderItemCommand>,
        couponId: Long? = null,
    ): Order {
        validateItems(items)

        // ✅ OrderItemSpec 준비
        val itemSpecs = items.map { cmd ->
            OrderItemSpec(
                product = Product.withId(cmd.productId, cmd.productName),
                quantity = cmd.quantity,
                price = cmd.price,
            )
        }

        // ✅ Order와 OrderItem을 함께 생성
        val order = Order.createWithItems(userId, couponId, itemSpecs)
        return orderRepository.save(order)
    }

    fun getOrdersByUserId(userId: Long, pageable: Pageable): Page<OrderedInfo> {
        return orderRepository.findByUserId(userId, pageable).map { OrderedInfo.from(it) }
    }

    fun getOrderById(userId: Long, orderId: Long): Order =
        orderRepository.findById(orderId)
            ?.takeIf { it.userId == userId }
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문이 존재하지 않습니다")

    fun getOrderByIdForAdmin(orderId: Long): Order =
        orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문이 존재하지 않습니다")

    private fun validateItems(items: List<CreateOrderItemCommand>) {
        if (items.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목은 최소 1개 이상이어야 합니다")
        }

        items.forEach { item ->
            if (item.quantity <= 0) {
                throw CoreException(ErrorType.BAD_REQUEST, "주문 수량은 0보다 커야 합니다")
            }
        }
    }
}
```

**주의:** Product.withId()는 Product 엔티티의 factory 메서드로 추가 필요

### 2.5 OrderFacade (변경 없음)

기존 로직 유지:
```kotlin
val order = orderService.createOrder(userId, createOrderItems, orderRequest.couponId)
```

OrderService 내부 구현이 변경되지만, Facade의 호출은 동일함.

---

## 3. 파일 변경 요약

| 파일 | 변경사항 | 복잡도 |
|------|---------|--------|
| `OrderItem.kt` | orderId 기본값 제거, Order 파라미터 추가, 검증 추가 | 중간 |
| `Order.kt` | addItem() internal화, createWithItems() 추가 | 중간 |
| `OrderItemSpec.kt` | **신규 생성** | 낮음 |
| `OrderService.kt` | OrderItemSpec 사용, createWithItems() 호출 | 중간 |
| `OrderFacade.kt` | 변경 없음 | - |

---

## 4. 테스트 영향

### 기존 테스트 수정 필요

**Order 생성:**
```kotlin
// Before
val order = Order.create(userId)
val item = OrderItem.create(orderId = order.id, productId = 1, ...)
order.addOrderItem(item)

// After
val order = Order.createWithItems(
    userId = userId,
    items = listOf(
        OrderItemSpec(product, quantity, price)
    )
)
```

### 새로운 테스트 케이스

1. **OrderItemSpec 생성** - 정상/비정상 케이스
2. **Order.createWithItems()** - 여러 item 생성 검증
3. **addItem() 제약** - internal 메서드 호출 불가 확인

---

## 5. 마이그레이션 전략

### Phase 1: 새 코드 추가
1. OrderItemSpec 생성
2. Order.createWithItems() 추가
3. OrderItem.create(order, ...) 오버로드

### Phase 2: OrderService 변경
1. 새로운 로직으로 변경
2. 기존 테스트 수정

### Phase 3: OrderItem 정리
1. orderId 기본값 제거
2. create() 메서드 시그니처 변경
3. 레거시 호출 제거

---

## 6. 위험 요소 및 완화 방안

| 위험 | 완화 방안 |
|------|---------|
| **기존 테스트 실패** | 모든 테스트를 단계별로 수정 및 검증 |
| **OrderItem 독립 생성** | protected constructor + create() 메서드만 사용 강제 |
| **Product 객체 필요** | OrderItemSpec에 product 인스턴스 저장 |

---

## 7. 기대 효과

- ✅ Aggregate 경계 명확화
- ✅ OrderItem의 완전한 생명주기 제어
- ✅ 부분 생성 상태 제거
- ✅ DDD 관점에서 Aggregate 강화
- ✅ 테스트 가능성 향상
