package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.loopers.application.service.DlqHandler
import com.loopers.application.service.ProductMetricsService
import com.loopers.domain.event.ProductViewedEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.Acknowledgment

@DisplayName("ProductViewedConsumer")
class ProductViewedConsumerTest {

    private lateinit var productMetricsService: ProductMetricsService
    private lateinit var dlqHandler: DlqHandler
    private lateinit var objectMapper: ObjectMapper
    private lateinit var consumer: ProductViewedConsumer
    private lateinit var acknowledgment: Acknowledgment

    @BeforeEach
    fun setUp() {
        productMetricsService = mockk()
        dlqHandler = mockk()
        objectMapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
        consumer = ProductViewedConsumer(productMetricsService, dlqHandler, objectMapper)
        acknowledgment = mockk()
    }

    @Test
    @DisplayName("product-events 메시지를 받아서 ProductMetricsService.processMetricsEvent를 호출한다")
    fun `handles product viewed events`() {
        // Given
        val event = ProductViewedEvent(
            productId = 123L,
            userId = 456L,
        )
        val payload = objectMapper.writeValueAsString(event)

        val record = ConsumerRecord<Any, Any>(
            "product-events",
            0,
            0L,
            "key",
            payload,
        )
        val messages = listOf(record)

        every { productMetricsService.processMetricsEvent(any()) } returns Unit
        every { acknowledgment.acknowledge() } returns Unit

        // When
        consumer.handleProductViewedEvents(messages, acknowledgment)

        // Then
        verify(exactly = 1) { productMetricsService.processMetricsEvent(any()) }
        verify(exactly = 1) { acknowledgment.acknowledge() }
        verify(exactly = 0) { dlqHandler.saveToDlq(any(), any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("메시지 처리 실패 시 DLQ에 기록하고 ACK하지 않는다")
    fun `handles processing failure and saves to DLQ`() {
        // Given
        val invalidPayload = "invalid json"

        val record = ConsumerRecord<Any, Any>(
            "product-events",
            0,
            0L,
            "key",
            invalidPayload,
        )
        val messages = listOf(record)

        every { dlqHandler.saveToDlq(any(), any(), any(), any(), any()) } returns mockk()

        // When
        consumer.handleProductViewedEvents(messages, acknowledgment)

        // Then
        verify(exactly = 1) {
            dlqHandler.saveToDlq(
                originalTopic = "product-events",
                messagePayload = invalidPayload,
                consumerGroup = "commerce-streamer-product-viewed",
                eventType = "ProductViewedEvent",
                exception = any(),
            )
        }
        verify(exactly = 0) { acknowledgment.acknowledge() }
    }

    @Test
    @DisplayName("배치의 일부 메시지 실패 시 전체 배치 ACK하지 않는다")
    fun `does not ACK batch if any message fails`() {
        // Given
        val validEvent = ProductViewedEvent(
            productId = 123L,
            userId = 456L,
        )
        val validPayload = objectMapper.writeValueAsString(validEvent)

        val validRecord = ConsumerRecord<Any, Any>(
            "product-events",
            0,
            0L,
            "key1",
            validPayload,
        )
        val invalidRecord = ConsumerRecord<Any, Any>(
            "product-events",
            0,
            1L,
            "key2",
            "invalid",
        )
        val messages = listOf(validRecord, invalidRecord)

        every { productMetricsService.processMetricsEvent(any()) } returns Unit
        every { dlqHandler.saveToDlq(any(), any(), any(), any(), any()) } returns mockk()

        // When
        consumer.handleProductViewedEvents(messages, acknowledgment)

        // Then
        verify(exactly = 1) { productMetricsService.processMetricsEvent(any()) }
        verify(exactly = 1) { dlqHandler.saveToDlq(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { acknowledgment.acknowledge() }
    }
}
