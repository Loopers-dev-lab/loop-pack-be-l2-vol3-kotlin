package com.loopers.interfaces.consumer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.loopers.application.coupon.CouponIssueRequestedEventHandler
import com.loopers.infrastructure.outbox.CouponIssueRequestedOutboxMessagePayload
import com.loopers.infrastructure.outbox.KafkaEventType
import com.loopers.infrastructure.outbox.KafkaOutboxEnvelope
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.kafka.support.Acknowledgment
import org.apache.kafka.clients.consumer.ConsumerRecord

@DisplayName("CouponIssueRequestConsumer")
class CouponIssueRequestConsumerTest {
    private val objectMapper = jacksonObjectMapper()
    private val eventHandler: CouponIssueRequestedEventHandler = mock()
    private val consumer = CouponIssueRequestConsumer(objectMapper, eventHandler)
    private val acknowledgment: Acknowledgment = mock()

    @Nested
    @DisplayName("유효한 coupon-issue-requests 메시지면")
    inner class WhenValidRecord {
        @Test
        @DisplayName("handler를 호출하고 ack한다")
        fun consume_success() {
            val payload = CouponIssueRequestedOutboxMessagePayload(
                requestId = 101L,
                couponId = 11L,
                userId = 1L,
            )
            val record = record(payload)

            consumer.consume(listOf(record), acknowledgment)

            verify(eventHandler).handle(payload)
            verify(acknowledgment).acknowledge()
        }
    }

    @Nested
    @DisplayName("handler가 실패하면")
    inner class WhenHandlerFails {
        @Test
        @DisplayName("ack하지 않고 예외를 전파한다")
        fun consume_failure() {
            val payload = CouponIssueRequestedOutboxMessagePayload(
                requestId = 101L,
                couponId = 11L,
                userId = 1L,
            )
            val record = record(payload)
            doThrow(IllegalStateException("boom")).`when`(eventHandler).handle(payload)

            assertThrows<IllegalStateException> {
                consumer.consume(listOf(record), acknowledgment)
            }

            verifyNoInteractions(acknowledgment)
        }
    }

    private fun record(payload: CouponIssueRequestedOutboxMessagePayload): ConsumerRecord<String, ByteArray> {
        val envelope = KafkaOutboxEnvelope(
            eventId = 1L,
            eventType = KafkaEventType.COUPON_ISSUE_REQUESTED,
            aggregateId = payload.couponId,
            payload = objectMapper.valueToTree(payload),
        )
        return ConsumerRecord(
            "coupon-issue-requests",
            0,
            0L,
            payload.couponId.toString(),
            objectMapper.writeValueAsBytes(envelope),
        )
    }
}
