package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.loopers.application.service.DlqHandler
import com.loopers.application.service.LikeCountService
import com.loopers.domain.event.LikeCountEvent
import com.loopers.domain.event.LikeCountEventType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.Acknowledgment

@DisplayName("LikeCountConsumer")
class LikeCountConsumerTest {

    private lateinit var likeCountService: LikeCountService
    private lateinit var dlqHandler: DlqHandler
    private lateinit var objectMapper: ObjectMapper
    private lateinit var consumer: LikeCountConsumer
    private lateinit var acknowledgment: Acknowledgment

    @BeforeEach
    fun setUp() {
        likeCountService = mockk()
        dlqHandler = mockk()
        objectMapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
        consumer = LikeCountConsumer(likeCountService, dlqHandler, objectMapper)
        acknowledgment = mockk()
    }

    @Test
    @DisplayName("like-events 메시지를 받아서 LikeCountService.processLikeCountEvent를 호출한다")
    fun `handles like count events`() {
        // Given
        val event = LikeCountEvent(
            productId = 100L,
            type = LikeCountEventType.INCREMENT,
            userId = 50L,
        )
        val payload = objectMapper.writeValueAsString(event)

        val record = ConsumerRecord<Any, Any>(
            "like-events",
            0,
            0L,
            "key",
            payload,
        )
        val messages = listOf(record)

        every { likeCountService.processLikeCountEvent(any()) } returns Unit
        every { acknowledgment.acknowledge() } returns Unit

        // When
        consumer.handleLikeCountEvents(messages, acknowledgment)

        // Then
        verify(exactly = 1) { likeCountService.processLikeCountEvent(any()) }
        verify(exactly = 1) { acknowledgment.acknowledge() }
        verify(exactly = 0) { dlqHandler.saveToDlq(any(), any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("메시지 처리 실패 시 DLQ에 기록하고 ACK한다")
    fun `handles processing failure and saves to DLQ`() {
        // Given
        val invalidPayload = "invalid json"

        val record = ConsumerRecord<Any, Any>(
            "like-events",
            0,
            0L,
            "key",
            invalidPayload,
        )
        val messages = listOf(record)

        every { dlqHandler.saveToDlq(any(), any(), any(), any(), any()) } returns mockk()
        every { acknowledgment.acknowledge() } returns Unit

        // When
        consumer.handleLikeCountEvents(messages, acknowledgment)

        // Then
        verify(exactly = 1) {
            dlqHandler.saveToDlq(
                originalTopic = "like-events",
                messagePayload = invalidPayload,
                consumerGroup = "commerce-streamer-like-count",
                eventType = "LikeCountEvent",
                exception = any(),
            )
        }
        // ✅ 에러 발생해도 ACK는 항상 수행됨
        verify(exactly = 1) { acknowledgment.acknowledge() }
    }

    @Test
    @DisplayName("배치의 일부 메시지 실패 시 전체 배치 ACK한다")
    fun `ACK batch even if any message fails`() {
        // Given
        val validEvent = LikeCountEvent(
            productId = 100L,
            type = LikeCountEventType.DECREMENT,
            userId = 50L,
        )
        val validPayload = objectMapper.writeValueAsString(validEvent)

        val validRecord = ConsumerRecord<Any, Any>(
            "like-events",
            0,
            0L,
            "key1",
            validPayload,
        )
        val invalidRecord = ConsumerRecord<Any, Any>(
            "like-events",
            0,
            1L,
            "key2",
            "invalid",
        )
        val messages = listOf(validRecord, invalidRecord)

        every { likeCountService.processLikeCountEvent(any()) } returns Unit
        every { dlqHandler.saveToDlq(any(), any(), any(), any(), any()) } returns mockk()
        every { acknowledgment.acknowledge() } returns Unit

        // When
        consumer.handleLikeCountEvents(messages, acknowledgment)

        // Then
        verify(exactly = 1) { likeCountService.processLikeCountEvent(any()) }
        verify(exactly = 1) { dlqHandler.saveToDlq(any(), any(), any(), any(), any()) }
        // ✅ 일부 메시지 실패해도 전체 배치 ACK
        verify(exactly = 1) { acknowledgment.acknowledge() }
    }
}
