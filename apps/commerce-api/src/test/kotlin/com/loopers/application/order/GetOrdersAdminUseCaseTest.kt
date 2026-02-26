package com.loopers.application.order

import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.order.FakeOrderItemRepository
import com.loopers.domain.order.FakeOrderRepository
import com.loopers.domain.order.OrderProductInfo
import com.loopers.domain.order.model.Order
import com.loopers.domain.common.vo.Quantity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class GetOrdersAdminUseCaseTest {

    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var orderItemRepository: FakeOrderItemRepository
    private lateinit var getOrdersAdminUseCase: GetOrdersAdminUseCase

    @BeforeEach
    fun setUp() {
        orderRepository = FakeOrderRepository()
        orderItemRepository = FakeOrderItemRepository()
        getOrdersAdminUseCase = GetOrdersAdminUseCase(orderRepository, orderItemRepository)
    }

    private fun createAndSaveOrder(userId: Long): Order {
        val order = Order.create(
            UserId(userId),
            listOf(OrderProductInfo(ProductId(1), "테스트 상품", Money(BigDecimal("10000"))) to Quantity(1)),
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
        @DisplayName("모든 주문이 페이지로 반환된다")
        fun execute_returnsPagedOrders() {
            // arrange
            createAndSaveOrder(1L)
            createAndSaveOrder(2L)

            // act
            val result = getOrdersAdminUseCase.execute(0, 10)

            // assert
            assertThat(result.totalElements).isEqualTo(2)
        }

        @Test
        @DisplayName("주문이 없으면 빈 페이지가 반환된다")
        fun execute_noOrders_returnsEmpty() {
            // act
            val result = getOrdersAdminUseCase.execute(0, 10)

            // assert
            assertThat(result.totalElements).isEqualTo(0)
            assertThat(result.content).isEmpty()
        }
    }
}
