package com.loopers.application.api.order

import com.loopers.application.admin.order.AdminOrderFacade
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.order.OrderService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.ZonedDateTime
import kotlin.test.assertEquals

@DisplayName("AdminOrderFacade 테스트")
class AdminOrderFacadeTest {

    private val orderService: OrderService = mockk()
    private val orderRepository: OrderRepository = mockk()
    private val adminOrderFacade = AdminOrderFacade(orderService, orderRepository)

    @Test
    @DisplayName("getOrders는 모든 주문을 페이지네이션으로 반환한다")
    fun testGetOrders() {
        // Arrange
        val pageable = PageRequest.of(0, 10)
        val mockOrder = mockk<Order>(relaxed = true)
        every { mockOrder.id } returns 1L
        every { mockOrder.userId } returns 100L
        every { mockOrder.getTotalPrice() } returns BigDecimal.valueOf(50000)
        every { mockOrder.orderItems } returns emptyList()
        every { mockOrder.createdAt } returns ZonedDateTime.now()

        val mockPage = PageImpl(listOf(mockOrder), pageable, 1)
        every { orderRepository.findOrders(pageable) } returns mockPage

        // Act
        val result = adminOrderFacade.getOrders(pageable)

        // Assert
        assertEquals(1, result.totalElements)
        assertEquals(1, result.content.size)
        assertEquals(100L, result.content[0].userId)
    }

    @Test
    @DisplayName("getOrderById는 특정 주문을 반환한다")
    fun testGetOrderById() {
        // Arrange
        val orderId = 1L
        val mockOrder = mockk<Order>(relaxed = true)
        every { mockOrder.id } returns orderId
        every { mockOrder.userId } returns 100L
        every { mockOrder.getTotalPrice() } returns BigDecimal.valueOf(50000)
        every { mockOrder.orderItems } returns emptyList()
        every { mockOrder.createdAt } returns ZonedDateTime.now()

        every { orderService.getOrderByIdForAdmin(orderId) } returns mockOrder

        // Act
        val result = adminOrderFacade.getOrderById(orderId)

        // Assert
        assertEquals(orderId, result.orderId)
        assertEquals(100L, result.userId)
    }
}
