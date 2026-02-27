package com.loopers.application.order

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.domain.common.SortOrder
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.OrderStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import java.time.ZonedDateTime

@ExtendWith(MockitoExtension::class)
class AdminOrderFacadeTest {

    @Mock
    private lateinit var orderService: OrderService

    private lateinit var adminOrderFacade: AdminOrderFacade

    @BeforeEach
    fun setUp() {
        adminOrderFacade = AdminOrderFacade(orderService)
    }

    @DisplayName("어드민 주문 목록 조회할 때,")
    @Nested
    inner class GetOrders {

        private val pageQuery = PageQuery(0, 20, SortOrder.UNSORTED)

        @DisplayName("주문 목록을 조회하면, AdminOrderInfo 페이지를 반환한다.")
        @Test
        fun returnsAdminOrderInfoPage() {
            // arrange
            val now = ZonedDateTime.now()
            val order = Order(userId = 1L)
            ReflectionTestUtils.setField(order, "id", 1L)
            ReflectionTestUtils.setField(order, "createdAt", now)

            val pageResult = PageResult(
                content = listOf(order),
                page = 0,
                size = 20,
                totalElements = 1,
                totalPages = 1,
            )

            whenever(orderService.getAllOrders(pageQuery)).thenReturn(pageResult)

            // act
            val result = adminOrderFacade.getOrders(pageQuery)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.content.first().orderId).isEqualTo(1L) },
                { assertThat(result.content.first().userId).isEqualTo(1L) },
                { assertThat(result.content.first().status).isEqualTo(OrderStatus.ORDERED) },
                { assertThat(result.content.first().orderedAt).isEqualTo(now) },
                { assertThat(result.totalElements).isEqualTo(1) },
            )
        }

        @DisplayName("주문이 없으면, 빈 페이지를 반환한다.")
        @Test
        fun returnsEmptyPage_whenNoOrders() {
            // arrange
            val emptyResult = PageResult<Order>(
                content = emptyList(),
                page = 0,
                size = 20,
                totalElements = 0,
                totalPages = 0,
            )

            whenever(orderService.getAllOrders(pageQuery)).thenReturn(emptyResult)

            // act
            val result = adminOrderFacade.getOrders(pageQuery)

            // assert
            assertAll(
                { assertThat(result.content).isEmpty() },
                { assertThat(result.totalElements).isEqualTo(0) },
            )
        }
    }
}
