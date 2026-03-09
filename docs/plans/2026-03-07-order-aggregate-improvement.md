# Order Aggregate 개선 구현 계획

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Order Aggregate의 경계를 강화하고 OrderItem의 생명주기를 Order Root가 완전히 제어하도록 개선

**Architecture:** Order Root가 OrderItem을 생성할 때 전달받는 방식에서, Order.createWithItems() Factory 메서드를 통해 Aggregate를 동시에 생성하도록 변경. OrderItemSpec을 중간 데이터 구조로 사용.

**Tech Stack:** Kotlin, Spring Boot 3.4.4, JPA/Hibernate, Testcontainers

---

## Task 1: OrderItemSpec DTO 생성

**Files:**
- Create: `apps/commerce-api/src/main/kotlin/com/loopers/domain/order/dto/OrderItemSpec.kt`

**Step 1: OrderItemSpec 파일 생성**

```kotlin
package com.loopers.domain.order.dto

import com.loopers.domain.product.Product
import java.math.BigDecimal

/**
 * OrderItem 생성용 DTO
 * Order.createWithItems()에 전달되는 중간 데이터 구조
 */
data class OrderItemSpec(
    val product: Product,
    val quantity: Int,
    val price: BigDecimal,
)
```

**Step 2: 컴파일 확인**

```bash
./gradlew :apps:commerce-api:compileKotlin
```

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add apps/commerce-api/src/main/kotlin/com/loopers/domain/order/dto/OrderItemSpec.kt
git commit -m "feat: add OrderItemSpec DTO for Order creation"
```

---

## Task 2: OrderItem 개선 - orderId 기본값 제거 및 검증 추가

**Files:**
- Modify: `apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderItem.kt`
- Test: `apps/commerce-api/src/test/kotlin/com/loopers/domain/order/OrderItemTest.kt`

**Step 1: 새 테스트 파일 생성 및 실패하는 테스트 작성**

```kotlin
package com.loopers.domain.order

import com.loopers.domain.product.Product
import com.loopers.support.error.CoreException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.assertEquals

class OrderItemTest {

    @Test
    fun `OrderItem 생성 - Order 인스턴스로 orderId 자동 설정`() {
        // Arrange
        val order = Order.create(userId = 1L)
        order.id = 100L  // 임시로 ID 설정 (실제로는 save 후)
        val product = Product.of(name = "상품명", price = BigDecimal("10000"))
        product.id = 1L

        // Act
        val item = OrderItem.create(order, product, 2, BigDecimal("10000"))

        // Assert
        assertEquals(100L, item.orderId)
        assertEquals(1L, item.productId)
        assertEquals("상품명", item.productName)
        assertEquals(2, item.quantity)
        assertEquals(BigDecimal("10000"), item.price)
    }

    @Test
    fun `OrderItem 생성 - 수량이 0 이하면 실패`() {
        // Arrange
        val order = Order.create(userId = 1L)
        order.id = 100L
        val product = Product.of(name = "상품명", price = BigDecimal("10000"))
        product.id = 1L

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            OrderItem.create(order, product, 0, BigDecimal("10000"))
        }
    }

    @Test
    fun `OrderItem 생성 - 가격이 0 이하면 실패`() {
        // Arrange
        val order = Order.create(userId = 1L)
        order.id = 100L
        val product = Product.of(name = "상품명", price = BigDecimal("10000"))
        product.id = 1L

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            OrderItem.create(order, product, 1, BigDecimal.ZERO)
        }
    }
}
```

**Step 2: 테스트 실행 (FAIL 예상)**

```bash
./gradlew :apps:commerce-api:test --tests OrderItemTest -v
```

Expected: FAIL - OrderItem.create(order, ...) 메서드 시그니처가 다름

**Step 3: OrderItem 수정**

현재 파일 읽기 (이미 읽은 내용):
```kotlin
companion object {
    fun create(
        orderId: Long = 0L,  // ❌ 제거할 부분
        productId: Long,
        quantity: Int,
        price: BigDecimal,
        productName: String,
    ): OrderItem { ... }
}
```

수정 후:
```kotlin
package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "order_items")
class OrderItem protected constructor(
    @Column(name = "order_id", nullable = false)
    val orderId: Long,  // ✅ 기본값 제거, 필수 파라미터
    @Column(name = "product_id", nullable = false)
    val productId: Long,
    @Column(name = "product_name", nullable = false, length = 200)
    val productName: String,
    @Column(nullable = false)
    val quantity: Int,
    @Column(nullable = false, precision = 19, scale = 2)
    val price: BigDecimal = BigDecimal.ZERO,
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

**Step 4: 테스트 실행 (PASS 예상)**

```bash
./gradlew :apps:commerce-api:test --tests OrderItemTest -v
```

Expected: PASS (3 tests)

**Step 5: Commit**

```bash
git add apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderItem.kt
git add apps/commerce-api/src/test/kotlin/com/loopers/domain/order/OrderItemTest.kt
git commit -m "feat: improve OrderItem - remove orderId default, add Order parameter and validation"
```

---

## Task 3: Order.createWithItems() Factory 메서드 추가

**Files:**
- Modify: `apps/commerce-api/src/main/kotlin/com/loopers/domain/order/Order.kt`
- Modify: `apps/commerce-api/src/test/kotlin/com/loopers/domain/order/OrderTest.kt`

**Step 1: 테스트 먼저 작성 (OrderTest 확장)**

기존 OrderTest에 새 테스트 메서드 추가:

```kotlin
@Test
fun `Order createWithItems - 여러 OrderItem을 함께 생성`() {
    // Arrange
    val userId = 1L
    val product1 = Product.of(name = "상품1", price = BigDecimal("10000"))
    product1.id = 1L
    val product2 = Product.of(name = "상품2", price = BigDecimal("20000"))
    product2.id = 2L

    val itemSpecs = listOf(
        OrderItemSpec(product1, 2, BigDecimal("10000")),
        OrderItemSpec(product2, 1, BigDecimal("20000")),
    )

    // Act
    val order = Order.createWithItems(userId, null, itemSpecs)

    // Assert
    assertEquals(userId, order.userId)
    assertEquals(2, order.orderItems.size)
    assertEquals(1L, order.orderItems[0].productId)
    assertEquals(2L, order.orderItems[1].productId)
    assertEquals(BigDecimal("40000"), order.getTotalPrice())
}

@Test
fun `Order createWithItems - 빈 items 리스트면 실패`() {
    // Act & Assert
    assertThrows<IllegalArgumentException> {
        Order.createWithItems(1L, null, emptyList())
    }
}
```

**Step 2: 테스트 실행 (FAIL 예상)**

```bash
./gradlew :apps:commerce-api:test --tests OrderTest -v
```

Expected: FAIL - Order.createWithItems() 메서드 없음

**Step 3: Order.kt에 createWithItems() 메서드 추가**

기존 companion object의 create() 메서드 아래에 추가:

```kotlin
companion object {
    fun create(userId: Long, couponId: Long? = null, status: OrderStatus = OrderStatus.PENDING): Order =
        Order(userId = userId, couponId = couponId)
            .apply {
                this.status = status
            }

    // ✅ 새 Factory 메서드
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
```

또한 Order의 addOrderItem() 메서드를 addItem()으로 이름 변경하고 internal로 변경:

```kotlin
// ✅ internal로 변경, 메서드명 단순화
internal fun addItem(product: Product, quantity: Int, price: BigDecimal) {
    val item = OrderItem.create(this, product, quantity, price)
    _orderItems.add(item)
}
```

Order.kt 전체 수정:

```kotlin
package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.domain.order.dto.OrderItemSpec
import com.loopers.domain.product.Product
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.ZonedDateTime

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

    // ✅ internal로 변경, 메서드명 단순화
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
        fun create(userId: Long, couponId: Long? = null, status: OrderStatus = OrderStatus.PENDING): Order =
            Order(userId = userId, couponId = couponId)
                .apply {
                    this.status = status
                }

        // ✅ 새 Factory 메서드
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

**Step 4: OrderItemSpec import 추가 확인**

Order.kt의 import 섹션에 OrderItemSpec이 포함되어 있는지 확인:
```kotlin
import com.loopers.domain.order.dto.OrderItemSpec
```

**Step 5: 테스트 실행 (PASS 예상)**

```bash
./gradlew :apps:commerce-api:test --tests OrderTest -v
```

Expected: PASS

**Step 6: Commit**

```bash
git add apps/commerce-api/src/main/kotlin/com/loopers/domain/order/Order.kt
git add apps/commerce-api/src/test/kotlin/com/loopers/domain/order/OrderTest.kt
git commit -m "feat: add Order.createWithItems() factory method and rename addOrderItem() to addItem()"
```

---

## Task 4: OrderService 변경 - Order.createWithItems() 사용

**Files:**
- Modify: `apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderService.kt`

**Step 1: OrderService 수정**

```kotlin
package com.loopers.domain.order

import com.loopers.domain.order.dto.CreateOrderItemCommand
import com.loopers.domain.order.dto.OrderItemSpec
import com.loopers.domain.order.dto.OrderedInfo
import com.loopers.domain.product.ProductService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OrderService(
    private val orderRepository: OrderRepository,
    private val productService: ProductService,  // ✅ Product 조회용 추가
) {

    @Transactional
    fun createOrder(userId: Long, items: List<CreateOrderItemCommand>, couponId: Long? = null): Order {
        validateItems(items)

        // ✅ OrderItemSpec 준비
        val itemSpecs = items.map { cmd ->
            OrderItemSpec(
                product = productService.getProduct(cmd.productId),
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

**Step 2: 컴파일 확인**

```bash
./gradlew :apps:commerce-api:compileKotlin
```

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderService.kt
git commit -m "refactor: use Order.createWithItems() in OrderService"
```

---

## Task 5: 기존 테스트 수정

**Files:**
- Modify: 주문 관련 모든 테스트 파일

**Step 1: OrderServiceTest 수정**

OrderService의 createOrder() 테스트가 있다면 다음과 같이 수정:

```kotlin
// 기존 테스트의 다음 부분을
val order = Order.create(userId)
val item = OrderItem.create(orderId = savedOrder.id, productId = 1, ...)

// 이렇게 변경
val product = Product.of(name = "테스트상품", price = BigDecimal("10000"))
product.id = 1L
val itemSpecs = listOf(
    OrderItemSpec(product, 2, BigDecimal("10000"))
)
val order = Order.createWithItems(userId, null, itemSpecs)
```

모든 관련 테스트 파일 찾기:
```bash
find apps/commerce-api/src/test -name "*OrderTest.kt" -o -name "*OrderServiceTest.kt" -o -name "*OrderFacadeTest.kt"
```

각 파일에서:
1. `OrderItem.create(orderId = ..., ...)` 호출 제거
2. `Order.addOrderItem()` 호출 제거
3. `Order.createWithItems()`로 변경

**Step 2: 모든 Order 관련 테스트 실행**

```bash
./gradlew :apps:commerce-api:test --tests "*Order*" -v
```

Expected: 모든 테스트 PASS

**Step 3: Commit**

```bash
git add apps/commerce-api/src/test
git commit -m "test: update order-related tests for new Order.createWithItems() factory"
```

---

## Task 6: OrderFacade 검증 (변경 없음 확인)

**Files:**
- Verify: `apps/commerce-api/src/main/kotlin/com/loopers/application/api/order/OrderFacade.kt`

**Step 1: OrderFacade 코드 검토**

OrderFacade의 createOrder() 메서드에서:
```kotlin
val order = orderService.createOrder(userId, createOrderItems, orderRequest.couponId)
```

이 부분이 변경 없이 동작하는지 확인 (OrderService 내부 구현이 변경되었으나 호출 인터페이스는 동일)

**Step 2: OrderFacade 테스트 실행**

```bash
./gradlew :apps:commerce-api:test --tests "*OrderFacadeTest*" -v
```

Expected: PASS (기존 테스트가 수정되었다면)

**Step 3: Commit 불필요**

OrderFacade는 변경 없으므로 commit 불필요.

---

## Task 7: 전체 통합 테스트 실행

**Step 1: 모든 Order 관련 테스트 실행**

```bash
./gradlew :apps:commerce-api:test --tests "*Order*" -v
```

Expected: 모든 테스트 PASS

**Step 2: 전체 API 테스트 실행**

```bash
./gradlew :apps:commerce-api:test -v
```

Expected: 모든 테스트 PASS

**Step 3: ktlint 검사**

```bash
./gradlew :apps:commerce-api:ktlintCheck
```

Expected: 모든 파일 OK

**Step 4: 빌드 확인**

```bash
./gradlew :apps:commerce-api:build
```

Expected: BUILD SUCCESSFUL

---

## Task 8: 최종 Commit 및 정리

**Step 1: 변경 사항 확인**

```bash
git status
```

Expected: 모든 변경이 commit되어 있음

**Step 2: 최종 로그 확인**

```bash
git log --oneline -10
```

Expected: Order Aggregate 개선 관련 commit들이 보임

**Step 3: 최종 Commit (설계 문서)**

```bash
git add docs/plans/2026-03-07-order-aggregate-design.md
git commit -m "docs: add Order Aggregate improvement design document"
```

---

## 주요 변경 사항 요약

| 파일 | 변경 | 이유 |
|------|------|------|
| `OrderItemSpec.kt` | 신규 생성 | Order 생성용 중간 데이터 구조 |
| `OrderItem.kt` | orderId 기본값 제거, Order 파라미터 추가 | Aggregate 경계 강화 |
| `Order.kt` | createWithItems() 추가, addItem() internal화 | 동시 생성, 캡슐화 |
| `OrderService.kt` | createWithItems() 사용 | Order와 OrderItem 동시 생성 |
| `테스트 파일들` | addOrderItem() → addItem() 변경 | 메서드명 변경 반영 |

---

## 롤백 방법

만약 문제가 발생하면:

```bash
# 마지막 8개 commit 중 특정 포인트로 돌아가기
git log --oneline -8
git reset --hard <commit_hash>
```

---

## 주의사항

1. **OrderItemSpec import**: 모든 OrderItemSpec을 사용하는 파일에서 import 추가 필요
2. **ProductService 의존성**: OrderService가 ProductService를 의존하게 됨 (이미 Facade에서 하던 것)
3. **테스트 수정**: 기존 Order 관련 테스트는 모두 수정 필요
4. **Backward Compatibility**: OrderItem.create()의 시그니처가 변경되므로 외부 호출도 확인 필요
