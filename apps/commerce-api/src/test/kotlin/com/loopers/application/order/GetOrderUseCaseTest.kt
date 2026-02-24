package com.loopers.application.order

import com.loopers.domain.common.Money
import com.loopers.domain.order.FakeOrderItemRepository
import com.loopers.domain.order.FakeOrderRepository
import com.loopers.domain.order.OrderProductInfo
import com.loopers.domain.order.model.Order
import com.loopers.domain.order.model.OrderItem
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

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
        val order = orderRepository.save(Order.create(userId, Money(BigDecimal("10000"))))
        val item = orderItemRepository.save(
            OrderItem.create(
                OrderProductInfo(1L, "테스트 상품", Money(BigDecimal("10000"))),
                1,
                order.id,
            ),
        )
        return order to listOf(item)
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
            val result = getOrderUseCase.execute(1L, order.id)

            // assert
            assertThat(result.id).isEqualTo(order.id)
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
                getOrderUseCase.execute(2L, order.id)
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
