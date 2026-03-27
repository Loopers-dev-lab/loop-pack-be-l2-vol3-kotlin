package com.loopers.domain.event.handler

import com.loopers.domain.event.OrderCreatedEvent
import com.loopers.domain.event.OrderLineItem
import com.loopers.infrastructure.productmetrics.ProductMetricsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("OrderCreatedEventHandler 테스트")
class OrderCreatedEventHandlerTest {

    private lateinit var productMetricsRepository: ProductMetricsRepository
    private lateinit var handler: OrderCreatedEventHandler

    @BeforeEach
    fun setUp() {
        productMetricsRepository = mockk()
        handler = OrderCreatedEventHandler(productMetricsRepository)
    }

    @Test
    @DisplayName("주문 생성 이벤트를 받으면 각 상품별 salesCount를 증가시킨다")
    fun shouldIncrementSalesCountForEachProduct() {
        // Given
        val lineItems = listOf(
            OrderLineItem(productId = 1L, quantity = 2),
            OrderLineItem(productId = 2L, quantity = 1),
        )
        val event = OrderCreatedEvent(
            orderId = 100L,
            lineItems = lineItems,
        )

        every { productMetricsRepository.incrementSalesCount(any(), any()) } returns Unit

        // When
        handler.handle(event)

        // Then
        verify { productMetricsRepository.incrementSalesCount(1L, 2) }
        verify { productMetricsRepository.incrementSalesCount(2L, 1) }
    }

    @Test
    @DisplayName("단일 상품 주문도 올바르게 처리한다")
    fun shouldHandleSingleProductOrder() {
        // Given
        val lineItems = listOf(OrderLineItem(productId = 5L, quantity = 3))
        val event = OrderCreatedEvent(orderId = 200L, lineItems = lineItems)

        every { productMetricsRepository.incrementSalesCount(5L, 3) } returns Unit

        // When
        handler.handle(event)

        // Then
        verify { productMetricsRepository.incrementSalesCount(5L, 3) }
    }

    @Test
    @DisplayName("동일 상품이 여러 번 주문되어도 각각 처리된다")
    fun shouldHandleDuplicateProductsInOrder() {
        // Given
        val lineItems = listOf(
            OrderLineItem(productId = 1L, quantity = 2),
            OrderLineItem(productId = 1L, quantity = 3),
        )
        val event = OrderCreatedEvent(orderId = 300L, lineItems = lineItems)

        every { productMetricsRepository.incrementSalesCount(any(), any()) } returns Unit

        // When
        handler.handle(event)

        // Then
        verify { productMetricsRepository.incrementSalesCount(1L, 2) }
        verify { productMetricsRepository.incrementSalesCount(1L, 3) }
    }
}
