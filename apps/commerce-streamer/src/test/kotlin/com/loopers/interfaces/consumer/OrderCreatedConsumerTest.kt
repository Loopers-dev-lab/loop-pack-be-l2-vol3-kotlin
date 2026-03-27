package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.loopers.application.service.DlqHandler
import com.loopers.application.service.ProductMetricsService
import com.loopers.domain.event.OrderCreatedEvent
import com.loopers.domain.event.OrderLineItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.Acknowledgment

@DisplayName("OrderCreatedConsumer")
class OrderCreatedConsumerTest {

    private lateinit var productMetricsService: ProductMetricsService
    private lateinit var dlqHandler: DlqHandler
    private lateinit var objectMapper: ObjectMapper
    private lateinit var consumer: OrderCreatedConsumer
    private lateinit var acknowledgment: Acknowledgment

    @BeforeEach
    fun setUp() {
        productMetricsService = mockk()
        dlqHandler = mockk()
        objectMapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
        consumer = OrderCreatedConsumer(productMetricsService, dlqHandler, objectMapper)
        acknowledgment = mockk()
    }

    @Test
    @DisplayName("order-events 메시지를 받아서 ProductMetricsService.processMetricsEvent를 호출한다")
    fun `handles order created events`() {
        // Given
        val event = OrderCreatedEvent(
            orderId = 100L,
            lineItems = listOf(
                OrderLineItem(productId = 1L, quantity = 2),
                OrderLineItem(productId = 2L, quantity = 1),
            ),
        )
        val payload = objectMapper.writeValueAsString(event)

        val record = ConsumerRecord<Any, Any>(
            "order-events",
            0,
            0L,
            "key",
            payload,
        )
        val messages = listOf(record)

        every { productMetricsService.processMetricsEvent(any()) } returns Unit
        every { acknowledgment.acknowledge() } returns Unit

        // When
        consumer.handleOrderCreatedEvents(messages, acknowledgment)

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
            "order-events",
            0,
            0L,
            "key",
            invalidPayload,
        )
        val messages = listOf(record)

        every { dlqHandler.saveToDlq(any(), any(), any(), any(), any()) } returns mockk()

        // When
        consumer.handleOrderCreatedEvents(messages, acknowledgment)

        // Then
        verify(exactly = 1) {
            dlqHandler.saveToDlq(
                originalTopic = "order-events",
                messagePayload = invalidPayload,
                consumerGroup = "commerce-streamer-order-created",
                eventType = "OrderCreatedEvent",
                exception = any(),
            )
        }
        verify(exactly = 0) { acknowledgment.acknowledge() }
    }

    @Test
    @DisplayName("배치의 일부 메시지 실패 시 전체 배치 ACK하지 않는다")
    fun `does not ACK batch if any message fails`() {
        // Given
        val validEvent = OrderCreatedEvent(
            orderId = 100L,
            lineItems = listOf(OrderLineItem(productId = 1L, quantity = 1)),
        )
        val validPayload = objectMapper.writeValueAsString(validEvent)

        val validRecord = ConsumerRecord<Any, Any>(
            "order-events",
            0,
            0L,
            "key1",
            validPayload,
        )
        val invalidRecord = ConsumerRecord<Any, Any>(
            "order-events",
            0,
            1L,
            "key2",
            "invalid",
        )
        val messages = listOf(validRecord, invalidRecord)

        every { productMetricsService.processMetricsEvent(any()) } returns Unit
        every { dlqHandler.saveToDlq(any(), any(), any(), any(), any()) } returns mockk()

        // When
        consumer.handleOrderCreatedEvents(messages, acknowledgment)

        // Then
        verify(exactly = 1) { productMetricsService.processMetricsEvent(any()) }
        verify(exactly = 1) { dlqHandler.saveToDlq(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { acknowledgment.acknowledge() }
    }
}
