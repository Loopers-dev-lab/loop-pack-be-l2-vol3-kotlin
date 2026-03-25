package com.loopers.interfaces.consumer

import com.loopers.application.service.ProductMetricsService
import com.loopers.domain.product.event.ProductViewedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.kafka.support.Acknowledgment
import org.junit.jupiter.api.assertDoesNotThrow
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

@DisplayName("ProductMetricsConsumer 통합 테스트")
class ProductMetricsConsumerTest {

    private lateinit var productMetricsService: ProductMetricsService
    private lateinit var objectMapper: ObjectMapper
    private lateinit var consumer: ProductMetricsConsumer
    private lateinit var acknowledgment: Acknowledgment

    @BeforeEach
    fun setUp() {
        productMetricsService = mockk()
        objectMapper = ObjectMapper().registerKotlinModule()
        consumer = ProductMetricsConsumer(productMetricsService, objectMapper)
        acknowledgment = mockk()
    }

    @Test
    @DisplayName("ProductViewedEvent 메시지를 처리한다")
    fun shouldProcessProductViewedEvent() {
        // Given
        val event = ProductViewedEvent(productId = 1L, userId = 1L)
        val payload = objectMapper.writeValueAsString(event)
        val record = ConsumerRecord<Any, Any>(
            "product.viewed",
            0,
            0L,
            "key",
            payload,
        )

        every { productMetricsService.processMetricsEvent(any(), any()) } returns Unit
        every { acknowledgment.acknowledge() } returns Unit

        // When
        assertDoesNotThrow {
            consumer.handleMetricsEvents(listOf(record), acknowledgment)
        }

        // Then
        verify { productMetricsService.processMetricsEvent(any(), event.dedupeKey) }
        verify { acknowledgment.acknowledge() }
    }

    @Test
    @DisplayName("처리 실패 시 acknowledgment하지 않는다")
    fun shouldNotAcknowledgeOnError() {
        // Given
        val record = ConsumerRecord<Any, Any>(
            "product.viewed",
            0,
            0L,
            "key",
            "invalid-json",
        )

        // When
        consumer.handleMetricsEvents(listOf(record), acknowledgment)

        // Then: No acknowledgment() call
        verify(exactly = 0) { acknowledgment.acknowledge() }
    }
}
