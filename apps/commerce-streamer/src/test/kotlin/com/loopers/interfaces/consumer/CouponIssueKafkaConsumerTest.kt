package com.loopers.interfaces.consumer

import com.loopers.application.coupon.CouponIssueRequestEventHandler
import com.loopers.config.kafka.event.CouponIssueRequestMessage
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.Acknowledgment
import java.time.ZonedDateTime

@DisplayName("CouponIssueKafkaConsumer")
class CouponIssueKafkaConsumerTest {
    private val couponIssueRequestEventHandler: CouponIssueRequestEventHandler = mockk()
    private val consumer = CouponIssueKafkaConsumer(couponIssueRequestEventHandler)

    @DisplayName("요청 처리 성공 후에만 manual ack를 호출한다")
    @Test
    fun acknowledgesAfterSuccess() {
        val message = couponIssueRequestMessage(requestId = 10L)
        val acknowledgment = mockk<Acknowledgment>(relaxed = true)
        every { couponIssueRequestEventHandler.handle(10L) } just runs

        consumer.couponIssueRequestListener(
            messages = listOf(recordOf("coupon:1", message)),
            acknowledgment = acknowledgment,
        )

        verify(exactly = 1) { couponIssueRequestEventHandler.handle(10L) }
        verify(exactly = 1) { acknowledgment.acknowledge() }
    }

    @DisplayName("요청 처리 중 예외가 나면 manual ack를 호출하지 않는다")
    @Test
    fun doesNotAcknowledgeWhenFailed() {
        val message = couponIssueRequestMessage(requestId = 11L)
        val acknowledgment = mockk<Acknowledgment>(relaxed = true)
        every { couponIssueRequestEventHandler.handle(11L) } throws IllegalStateException("processing failed")

        assertThatThrownBy {
            consumer.couponIssueRequestListener(
                messages = listOf(recordOf("coupon:1", message)),
                acknowledgment = acknowledgment,
            )
        }.isInstanceOf(IllegalStateException::class.java)

        verify(exactly = 0) { acknowledgment.acknowledge() }
    }

    private fun recordOf(
        key: String,
        event: CouponIssueRequestMessage,
    ): ConsumerRecord<String, CouponIssueRequestMessage> {
        return ConsumerRecord("coupon-issue-requests", 0, 0L, key, event)
    }

    private fun couponIssueRequestMessage(requestId: Long): CouponIssueRequestMessage {
        return CouponIssueRequestMessage(
            eventId = "event-$requestId",
            requestId = requestId,
            couponId = 1L,
            userId = 99L,
            requestedAt = ZonedDateTime.now(),
        )
    }
}
