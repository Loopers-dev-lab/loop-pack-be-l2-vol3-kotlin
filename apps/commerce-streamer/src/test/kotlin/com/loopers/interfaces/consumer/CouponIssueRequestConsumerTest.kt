package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.loopers.application.consumer.DeadLetterPublisher
import com.loopers.application.consumer.EventHandledRecorder
import com.loopers.application.coupon.CouponIssueRequestProcessor
import com.loopers.kafka.CouponIssueRequestedPayload
import com.loopers.kafka.IntegrationEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.Acknowledgment
import java.time.ZonedDateTime

class CouponIssueRequestConsumerTest {
    private val objectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(KotlinModule.Builder().build())

    @Test
    fun `쿠폰_발급_요청을_processor에_위임한다`() {
        val eventHandledRecorder = mockk<EventHandledRecorder>()
        val processor = mockk<CouponIssueRequestProcessor>(relaxed = true)
        val deadLetterPublisher = mockk<DeadLetterPublisher>(relaxed = true)
        val acknowledgment = mockk<Acknowledgment>(relaxed = true)

        every { eventHandledRecorder.markHandled(any(), any()) } returns true

        val consumer = CouponIssueRequestConsumer(
            objectMapper = objectMapper,
            eventHandledRecorder = eventHandledRecorder,
            couponIssueRequestProcessor = processor,
            deadLetterPublisher = deadLetterPublisher,
        )

        consumer.consume(
            message = messageFor(
                eventId = "coupon-issue-requested:1",
                payload = CouponIssueRequestedPayload(
                    requestId = 1L,
                    couponId = 10L,
                    memberId = 20L,
                    requestedAt = ZonedDateTime.now(),
                ),
            ),
            acknowledgment = acknowledgment,
        )

        verify {
            processor.process(
                requestId = 1L,
                couponId = 10L,
                memberId = 20L,
            )
        }
        verify { acknowledgment.acknowledge() }
    }

    @Test
    fun `processor가_예외를_던지면_DLQ로_보낸다`() {
        val eventHandledRecorder = mockk<EventHandledRecorder>()
        val processor = mockk<CouponIssueRequestProcessor>()
        val deadLetterPublisher = mockk<DeadLetterPublisher>(relaxed = true)
        val acknowledgment = mockk<Acknowledgment>(relaxed = true)

        every { eventHandledRecorder.markHandled(any(), any()) } returns true
        every { processor.process(any(), any(), any()) } throws IllegalStateException("boom")

        val message = messageFor(
            eventId = "coupon-issue-requested:1",
            payload = CouponIssueRequestedPayload(
                requestId = 1L,
                couponId = 10L,
                memberId = 20L,
                requestedAt = ZonedDateTime.now(),
            ),
        )

        val consumer = CouponIssueRequestConsumer(
            objectMapper = objectMapper,
            eventHandledRecorder = eventHandledRecorder,
            couponIssueRequestProcessor = processor,
            deadLetterPublisher = deadLetterPublisher,
        )

        consumer.consume(
            message = message,
            acknowledgment = acknowledgment,
        )

        verify {
            deadLetterPublisher.publish(
                sourceTopic = "coupon-issue-requests",
                key = "10",
                payload = message,
                cause = any(),
            )
        }
        verify { acknowledgment.acknowledge() }
    }

    private fun messageFor(
        eventId: String,
        payload: CouponIssueRequestedPayload,
    ): String {
        return objectMapper.writeValueAsString(
            IntegrationEvent(
                eventId = eventId,
                eventType = "CouponIssueRequested",
                aggregateType = "coupon",
                aggregateId = payload.couponId.toString(),
                key = payload.couponId.toString(),
                version = 1L,
                occurredAt = ZonedDateTime.now(),
                payload = payload,
            ),
        )
    }
}
