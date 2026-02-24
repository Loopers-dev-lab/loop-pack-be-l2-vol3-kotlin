package com.loopers.application.order

import com.loopers.domain.common.Money
import com.loopers.domain.order.FakeOrderItemRepository
import com.loopers.domain.order.FakeOrderRepository
import com.loopers.domain.order.OrderProductInfo
import com.loopers.domain.order.model.Order
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class GetOrderAdminUseCaseTest {

    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var orderItemRepository: FakeOrderItemRepository
    private lateinit var getOrderAdminUseCase: GetOrderAdminUseCase

    @BeforeEach
    fun setUp() {
        orderRepository = FakeOrderRepository()
        orderItemRepository = FakeOrderItemRepository()
        getOrderAdminUseCase = GetOrderAdminUseCase(orderRepository, orderItemRepository)
    }

    private fun createAndSaveOrder(userId: Long): Order {
        val order = Order.create(
            userId,
            listOf(OrderProductInfo(1L, "테스트 상품", Money(BigDecimal("10000"))) to 1),
        )
        val savedOrder = orderRepository.save(order)
        order.assignOrderIdToItems(savedOrder.id)
        order.items.forEach { orderItemRepository.save(it) }
        return savedOrder
    }

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("주문이 존재하면 소유자와 관계없이 OrderInfo가 반환된다")
        fun execute_existingOrder_returnsOrderInfo() {
            // arrange
            val order = createAndSaveOrder(1L)

            // act
            val result = getOrderAdminUseCase.execute(order.id)

            // assert
            assertThat(result.id).isEqualTo(order.id)
            assertThat(result.items).hasSize(1)
        }

        @Test
        @DisplayName("존재하지 않는 주문을 조회하면 NOT_FOUND 예외가 발생한다")
        fun execute_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                getOrderAdminUseCase.execute(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
