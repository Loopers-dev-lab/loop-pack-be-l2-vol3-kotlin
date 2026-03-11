package com.loopers.application.order

import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.order.FakeOrderItemRepository
import com.loopers.domain.order.FakeOrderRepository
import com.loopers.domain.order.OrderProductData
import com.loopers.domain.order.model.Order
import com.loopers.domain.order.model.OrderItem
import com.loopers.domain.common.vo.Quantity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.ZonedDateTime

class GetOrderUseCaseTest {

    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var orderItemRepository: FakeOrderItemRepository
    private lateinit var getOrderUseCase: GetOrderUseCase

    @BeforeEach
    fun setUp() {
        orderRepository = FakeOrderRepository()
        orderItemRepository = FakeOrderItemRepository()
        getOrderUseCase = GetOrderUseCase(orderRepository, orderItemRepository)
    }

    private fun createAndSaveOrder(userId: Long): Pair<Order, List<OrderItem>> {
        val order = Order.create(
            UserId(userId),
            listOf(OrderProductData(ProductId(1), "테스트 상품", Money(BigDecimal("10000"))) to Quantity(1)),
        )
        val savedOrder = orderRepository.save(order)
        order.assignOrderIdToItems(savedOrder.id)
        val savedItems = order.items.map { orderItemRepository.save(it) }
        return savedOrder to savedItems
    }

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("주문 소유자가 조회하면 OrderInfo가 반환된다")
        fun execute_owner_returnsOrderInfo() {
            // arrange
            val (order, _) = createAndSaveOrder(1L)

            // act
            val result = getOrderUseCase.execute(1L, order.id.value)

            // assert
            assertThat(result.id).isEqualTo(order.id.value)
            assertThat(result.userId).isEqualTo(1L)
            assertThat(result.items).hasSize(1)
        }

        @Test
        @DisplayName("다른 사용자가 조회하면 NOT_FOUND 예외가 발생한다")
        fun execute_otherUser_throwsNotFound() {
            // arrange
            val (order, _) = createAndSaveOrder(1L)

            // act
            val exception = assertThrows<CoreException> {
                getOrderUseCase.execute(2L, order.id.value)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("삭제된 주문을 조회하면 NOT_FOUND 예외가 발생한다")
        fun execute_deletedOrder_throwsNotFound() {
            // arrange
            val deletedOrder = Order.fromPersistence(
                id = OrderId(0),
                refUserId = UserId(1),
                status = Order.OrderStatus.CREATED,
                originalPrice = Money(BigDecimal("10000")),
                discountAmount = Money(BigDecimal.ZERO),
                totalPrice = Money(BigDecimal("10000")),
                refCouponId = null,
                deletedAt = ZonedDateTime.now(),
            )
            val saved = orderRepository.save(deletedOrder)

            // act
            val exception = assertThrows<CoreException> {
                getOrderUseCase.execute(1L, saved.id.value)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("존재하지 않는 주문을 조회하면 NOT_FOUND 예외가 발생한다")
        fun execute_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                getOrderUseCase.execute(1L, 999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
