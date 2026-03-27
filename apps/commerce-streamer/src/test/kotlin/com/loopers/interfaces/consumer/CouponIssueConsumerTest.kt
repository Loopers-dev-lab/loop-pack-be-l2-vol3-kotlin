package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.loopers.application.service.CouponIssueService
import com.loopers.application.service.DlqHandler
import com.loopers.domain.event.CouponIssueRequestedEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.Acknowledgment
import java.time.ZonedDateTime

@DisplayName("CouponIssueConsumer")
class CouponIssueConsumerTest {

    private lateinit var couponIssueService: CouponIssueService
    private lateinit var dlqHandler: DlqHandler
    private lateinit var objectMapper: ObjectMapper
    private lateinit var consumer: CouponIssueConsumer
    private lateinit var acknowledgment: Acknowledgment

    @BeforeEach
    fun setUp() {
        couponIssueService = mockk()
        dlqHandler = mockk()
        objectMapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
        consumer = CouponIssueConsumer(couponIssueService, dlqHandler, objectMapper)
        acknowledgment = mockk()
    }

    @DisplayName("정상 메시지 처리 시 ACK 한다")
    @Test
    fun handleCouponIssueEvents_successWithAck() {
        // Given
        val event = CouponIssueRequestedEvent(
            userId = 1L,
            templateId = 1L,
            requestedAt = ZonedDateTime.now(),
        )
        val payload = objectMapper.writeValueAsString(event)
        val record = ConsumerRecord<Any, Any>(
            "coupon-events",
            0,
            0L,
            "key",
            payload,
        )

        every { couponIssueService.processIssuanceRequest(any()) } returns Unit
        every { acknowledgment.acknowledge() } returns Unit

        // When
        consumer.handleCouponIssueEvents(listOf(record), acknowledgment)

        // Then
        verify(exactly = 1) { couponIssueService.processIssuanceRequest(any()) }
        verify(exactly = 1) { acknowledgment.acknowledge() }
    }

    @DisplayName("처리 실패 시 DLQ로 이동하고 ACK한다")
    @Test
    fun handleCouponIssueEvents_failureWithDlqAndAck() {
        // Given
        val event = CouponIssueRequestedEvent(
            userId = 1L,
            templateId = 1L,
            requestedAt = ZonedDateTime.now(),
        )
        val payload = objectMapper.writeValueAsString(event)
        val record = ConsumerRecord<Any, Any>(
            "coupon-events",
            0,
            0L,
            "key",
            payload,
        )

        val exception = RuntimeException("처리 실패")
        every { couponIssueService.processIssuanceRequest(any()) } throws exception
        every { dlqHandler.saveToDlq(any(), any(), any(), any(), any()) } returns mockk()
        every { acknowledgment.acknowledge() } returns Unit

        // When
        consumer.handleCouponIssueEvents(listOf(record), acknowledgment)

        // Then
        verify(exactly = 1) { couponIssueService.processIssuanceRequest(any()) }
        verify(exactly = 1) { dlqHandler.saveToDlq(any(), any(), any(), any(), any()) }
        verify(exactly = 1) { acknowledgment.acknowledge() } // ✅ DLQ 저장 후 ACK
    }

    @DisplayName("여러 메시지 중 일부 실패 시 DLQ 저장 후 ACK한다")
    @Test
    fun handleCouponIssueEvents_partialFailureWithDlqAndAck() {
        // Given
        val event1 = CouponIssueRequestedEvent(
            userId = 1L,
            templateId = 1L,
            requestedAt = ZonedDateTime.now(),
        )
        val event2 = CouponIssueRequestedEvent(
            userId = 2L,
            templateId = 1L,
            requestedAt = ZonedDateTime.now(),
        )
        val payload1 = objectMapper.writeValueAsString(event1)
        val payload2 = objectMapper.writeValueAsString(event2)

        val record1 = ConsumerRecord<Any, Any>("coupon-events", 0, 0L, "key1", payload1)
        val record2 = ConsumerRecord<Any, Any>("coupon-events", 0, 1L, "key2", payload2)

        every { couponIssueService.processIssuanceRequest(any()) } throws RuntimeException("실패")
        every { dlqHandler.saveToDlq(any(), any(), any(), any(), any()) } returns mockk()
        every { acknowledgment.acknowledge() } returns Unit

        // When
        consumer.handleCouponIssueEvents(listOf(record1, record2), acknowledgment)

        // Then
        verify(exactly = 2) { dlqHandler.saveToDlq(any(), any(), any(), any(), any()) }
        verify(exactly = 1) { acknowledgment.acknowledge() } // ✅ 배치 전체 처리 후 ACK
    }

    @DisplayName("역직렬화 실패 시 DLQ로 이동하고 ACK한다")
    @Test
    fun handleCouponIssueEvents_invalidJsonWithDlqAndAck() {
        // Given
        val record = ConsumerRecord<Any, Any>(
            "coupon-events",
            0,
            0L,
            "key",
            "invalid-json",
        )

        every { dlqHandler.saveToDlq(any(), any(), any(), any(), any()) } returns mockk()
        every { acknowledgment.acknowledge() } returns Unit

        // When
        consumer.handleCouponIssueEvents(listOf(record), acknowledgment)

        // Then
        verify(exactly = 1) { dlqHandler.saveToDlq(any(), any(), any(), any(), any()) }
        verify(exactly = 1) { acknowledgment.acknowledge() } // ✅ DLQ 저장 후 ACK
    }

    @DisplayName("DLQ 저장 시 올바른 파라미터를 전달하고 ACK한다")
    @Test
    fun handleCouponIssueEvents_dlqParametersCorrectAndAck() {
        // Given
        val event = CouponIssueRequestedEvent(
            userId = 1L,
            templateId = 1L,
            requestedAt = ZonedDateTime.now(),
        )
        val payload = objectMapper.writeValueAsString(event)
        val record = ConsumerRecord<Any, Any>(
            "coupon-events",
            0,
            0L,
            "key",
            payload,
        )

        val exception = RuntimeException("테스트 에러")
        every { couponIssueService.processIssuanceRequest(any()) } throws exception

        val dlqCaptor = slot<String>()
        val topicCaptor = slot<String>()
        val groupCaptor = slot<String>()
        val typeCaptor = slot<String>()

        every {
            dlqHandler.saveToDlq(
                capture(topicCaptor),
                capture(dlqCaptor),
                capture(groupCaptor),
                capture(typeCaptor),
                any(),
            )
        } returns mockk()
        every { acknowledgment.acknowledge() } returns Unit

        // When
        consumer.handleCouponIssueEvents(listOf(record), acknowledgment)

        // Then
        assert(topicCaptor.captured == "coupon-events")
        assert(dlqCaptor.captured == payload)
        assert(groupCaptor.captured == "commerce-streamer-coupon")
        assert(typeCaptor.captured == "CouponIssueRequestedEvent")
        verify(exactly = 1) { acknowledgment.acknowledge() } // ✅ DLQ 저장 후 ACK
    }
}
