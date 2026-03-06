package com.loopers.domain.order

import com.loopers.domain.order.dto.CreateOrderItemCommand
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import org.junit.jupiter.api.assertThrows

@DisplayName("OrderService")
class OrderServiceTest {
    private val orderRepository: OrderRepository = mockk()
    private val orderService = OrderService(
        orderRepository = orderRepository,
    )

    @DisplayName("유효한 정보가 주어지면 주문이 생성되고 저장된다")
    @Test
    fun createsOrder_success() {
        // arrange
        val userId = 1L
        val orderId = 100L

        val createOrderItemCommand = CreateOrderItemCommand(
            productId = 1L,
            productName = "테스트 상품",
            quantity = 2,
            price = BigDecimal("10000.00"),
        )

        val orderSlot = slot<Order>()
        every { orderRepository.save(capture(orderSlot)) } answers {
            orderSlot.captured.apply {
                val idField = Order::class.java.superclass?.getDeclaredField("id")
                idField?.isAccessible = true
                idField?.set(this, orderId)
            }
        }

        // act
        val result = orderService.createOrder(
            userId = userId,
            items = listOf(createOrderItemCommand),
            couponId = null,
        )

        // assert
        assertThat(result.id).isEqualTo(orderId)
    }

    @DisplayName("주문 항목이 없으면 BAD_REQUEST 예외를 던진다")
    @Test
    fun createsOrder_throwsException_whenItemsIsEmpty() {
        // arrange
        val userId = 1L

        // act & assert
        assertThatThrownBy {
            orderService.createOrder(userId = userId, items = emptyList())
        }
            .isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
    }

    @DisplayName("수량이 0 이하면 BAD_REQUEST 예외를 던진다")
    @Test
    fun createsOrder_throwsException_whenQuantityIsInvalid() {
        // arrange
        val userId = 1L
        val createOrderItemCommand = CreateOrderItemCommand(
            productId = 1L,
            productName = "테스트 상품",
            quantity = 0,
            price = BigDecimal("10000.00"),
        )

        // act & assert
        assertThatThrownBy {
            orderService.createOrder(userId = userId, items = listOf(createOrderItemCommand))
        }
            .isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
    }

    @DisplayName("OrderItem에 할인액이 설정되어 총액에 반영된다")
    @Test
    fun orderItem_appliesDiscount_whenDiscountAmountSet() {
        // arrange
        val item = OrderItem.create(
            orderId = 1L,
            productId = 1L,
            quantity = 2,
            price = BigDecimal("10000"),
            productName = "Test Product",
        )

        // act
        item.applyDiscountAmount(BigDecimal("2000"))

        // assert
        assertThat(item.getSubtotal()).isEqualTo(BigDecimal("18000"))
    }

    @DisplayName("OrderItem에 음수 할인액을 설정하면 예외 발생")
    @Test
    fun orderItem_throwsException_whenNegativeDiscountAmount() {
        // arrange
        val item = OrderItem.create(
            orderId = 1L,
            productId = 1L,
            quantity = 1,
            price = BigDecimal("10000"),
            productName = "Test Product",
        )

        // act & assert
        assertThrows<CoreException> {
            item.applyDiscountAmount(BigDecimal("-1000"))
        }
    }

    @DisplayName("쿠폰이 있으면 주문 생성 시 couponId가 저장된다")
    @Test
    fun createsOrder_savesWithCouponId_whenCouponIdProvided() {
        // arrange
        val userId = 1L
        val couponId = 5L
        val orderId = 100L

        val createOrderItemCommand = CreateOrderItemCommand(
            productId = 1L,
            productName = "테스트 상품",
            quantity = 1,
            price = BigDecimal("10000.00"),
        )

        val orderSlot = slot<Order>()
        every { orderRepository.save(capture(orderSlot)) } answers {
            orderSlot.captured.apply {
                val idField = Order::class.java.superclass?.getDeclaredField("id")
                idField?.isAccessible = true
                idField?.set(this, orderId)
            }
        }

        // act
        val result = orderService.createOrder(
            userId = userId,
            items = listOf(createOrderItemCommand),
            couponId = couponId,
        )

        // assert
        assertThat(result.id).isEqualTo(orderId)
        assertThat(orderSlot.captured.couponId).isEqualTo(couponId)
    }
}
