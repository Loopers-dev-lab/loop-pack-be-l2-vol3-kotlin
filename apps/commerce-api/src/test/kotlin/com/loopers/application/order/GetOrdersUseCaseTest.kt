package com.loopers.application.order

import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.order.FakeOrderItemRepository
import com.loopers.domain.order.FakeOrderRepository
import com.loopers.domain.order.OrderProductData
import com.loopers.domain.order.model.Order
import com.loopers.domain.common.vo.Quantity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZonedDateTime

class GetOrdersUseCaseTest {

    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var orderItemRepository: FakeOrderItemRepository
    private lateinit var getOrdersUseCase: GetOrdersUseCase

    @BeforeEach
    fun setUp() {
        orderRepository = FakeOrderRepository()
        orderItemRepository = FakeOrderItemRepository()
        getOrdersUseCase = GetOrdersUseCase(orderRepository, orderItemRepository)
    }

    private fun createAndSaveOrder(userId: Long): Order {
        val order = Order.create(
            UserId(userId),
            listOf(OrderProductData(ProductId(1), "테스트 상품", Money(BigDecimal("10000"))) to Quantity(1)),
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
        @DisplayName("해당 사용자의 주문만 반환된다")
        fun execute_returnsOnlyUserOrders() {
            // arrange
            createAndSaveOrder(1L)
            createAndSaveOrder(1L)
            createAndSaveOrder(2L)

            val from = ZonedDateTime.now().minusDays(1)
            val to = ZonedDateTime.now().plusDays(1)

            // act
            val result = getOrdersUseCase.execute(1L, from, to, 0, 10)

            // assert
            assertThat(result.totalElements).isEqualTo(2)
            assertThat(result.content).allMatch { it.userId == 1L }
        }

        @Test
        @DisplayName("기간 범위 밖의 주문은 제외된다")
        fun execute_excludesOutOfRange() {
            // arrange
            createAndSaveOrder(1L)

            val from = ZonedDateTime.now().plusDays(1)
            val to = ZonedDateTime.now().plusDays(2)

            // act
            val result = getOrdersUseCase.execute(1L, from, to, 0, 10)

            // assert
            assertThat(result.totalElements).isEqualTo(0)
            assertThat(result.content).isEmpty()
        }

        @Test
        @DisplayName("주문이 없으면 빈 페이지가 반환된다")
        fun execute_noOrders_returnsEmpty() {
            // arrange
            val from = ZonedDateTime.now().minusDays(1)
            val to = ZonedDateTime.now().plusDays(1)

            // act
            val result = getOrdersUseCase.execute(1L, from, to, 0, 10)

            // assert
            assertThat(result.totalElements).isEqualTo(0)
            assertThat(result.content).isEmpty()
        }

        @Test
        @DisplayName("페이지네이션이 올바르게 동작한다")
        fun execute_pagination_works() {
            // arrange
            repeat(5) { createAndSaveOrder(1L) }

            val from = ZonedDateTime.now().minusDays(1)
            val to = ZonedDateTime.now().plusDays(1)

            // act
            val page0 = getOrdersUseCase.execute(1L, from, to, 0, 2)
            val page1 = getOrdersUseCase.execute(1L, from, to, 1, 2)

            // assert
            assertThat(page0.totalElements).isEqualTo(5)
            assertThat(page0.content).hasSize(2)
            assertThat(page1.content).hasSize(2)
        }
    }
}
