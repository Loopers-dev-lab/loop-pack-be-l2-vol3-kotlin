package com.loopers.application.service

import com.loopers.domain.eventhandled.EventHandled
import com.loopers.domain.eventhandled.EventHandledRepository
import com.loopers.domain.product.event.ProductViewedEvent
import com.loopers.domain.productmetrics.ProductMetricsRepository
import com.loopers.interfaces.consumer.EventHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("ProductMetricsService 단위 테스트")
class ProductMetricsServiceTest {

    private lateinit var productMetricsRepository: ProductMetricsRepository
    private lateinit var eventHandledRepository: EventHandledRepository
    private lateinit var handlers: Map<String, EventHandler>
    private lateinit var service: ProductMetricsService

    @BeforeEach
    fun setUp() {
        productMetricsRepository = mockk()
        eventHandledRepository = mockk()
        handlers = mapOf(
            "ProductViewedEvent" to mockk<EventHandler>(),
        )
        service = ProductMetricsService(productMetricsRepository, eventHandledRepository, handlers)
    }

    @Test
    @DisplayName("이미 처리된 이벤트는 무시한다")
    fun shouldIgnoreDuplicateEvents() {
        // Given
        val dedupeKey = "test-key-123"
        val event = ProductViewedEvent(productId = 1L, userId = 1L, dedupeKey = dedupeKey)

        every { eventHandledRepository.existsByDedupeKey(dedupeKey) } returns true

        // When
        service.processMetricsEvent(event, dedupeKey)

        // Then
        verify(exactly = 0) { handlers["ProductViewedEvent"]?.handle(any()) }
        verify(exactly = 0) { eventHandledRepository.save(any()) }
    }

    @Test
    @DisplayName("처리되지 않은 이벤트는 핸들러를 호출한다")
    fun shouldProcessNewEvents() {
        // Given
        val dedupeKey = "test-key-123"
        val event = ProductViewedEvent(productId = 1L, userId = 1L, dedupeKey = dedupeKey)
        val handler = mockk<EventHandler>()

        every { eventHandledRepository.existsByDedupeKey(dedupeKey) } returns false
        every { handler.handle(event) } returns Unit
        every { eventHandledRepository.save(any()) } returns EventHandled(dedupeKey = dedupeKey)

        val serviceWithHandler = ProductMetricsService(
            productMetricsRepository,
            eventHandledRepository,
            mapOf("ProductViewedEvent" to handler),
        )

        // When
        serviceWithHandler.processMetricsEvent(event, dedupeKey)

        // Then
        verify { handler.handle(event) }
        verify { eventHandledRepository.save(any()) }
    }
}
