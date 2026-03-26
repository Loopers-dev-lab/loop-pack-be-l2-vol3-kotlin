package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.loopers.application.consumer.EventHandledRecorder
import com.loopers.infrastructure.coupon.CouponIssueRequestEntity
import com.loopers.infrastructure.coupon.CouponIssueRequestJpaRepository
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.coupon.IssuedCouponEntity
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import com.loopers.kafka.CouponIssueRequestedPayload
import com.loopers.kafka.IntegrationEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.Acknowledgment
import java.time.ZonedDateTime
import java.util.Optional

class CouponIssueRequestConsumerTest {
    private val objectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(KotlinModule.Builder().build())

    @Test
    fun `쿠폰_발급_요청이_성공하면_요청_상태를_SUCCEEDED로_바꾼다`() {
        val request = CouponIssueRequestEntity(
            id = 1L,
            couponId = 10L,
            memberId = 20L,
            status = "PENDING",
            requestedAt = ZonedDateTime.now(),
        )
        val eventHandledRecorder = mockk<EventHandledRecorder>()
        val requestRepository = mockk<CouponIssueRequestJpaRepository>(relaxed = true)
        val couponRepository = mockk<CouponJpaRepository>()
        val issuedCouponRepository = mockk<IssuedCouponJpaRepository>()
        val acknowledgment = mockk<Acknowledgment>(relaxed = true)

        every { eventHandledRecorder.markHandled(any(), any()) } returns true
        every { requestRepository.findById(1L) } returns Optional.of(request)
        every { requestRepository.save(any()) } answers { firstArg() }
        every { issuedCouponRepository.existsByCouponIdAndMemberId(10L, 20L) } returns false
        every { couponRepository.tryIncreaseIssuedCount(10L) } returns 1
        every { issuedCouponRepository.save(any()) } answers {
            val entity = firstArg<IssuedCouponEntity>()
            IssuedCouponEntity(
                id = 999L,
                couponId = entity.couponId,
                memberId = entity.memberId,
                status = entity.status,
                issuedAt = entity.issuedAt,
            )
        }

        val consumer = CouponIssueRequestConsumer(
            objectMapper = objectMapper,
            eventHandledRecorder = eventHandledRecorder,
            couponIssueRequestJpaRepository = requestRepository,
            couponJpaRepository = couponRepository,
            issuedCouponJpaRepository = issuedCouponRepository,
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

        assertThat(request.status).isEqualTo("SUCCEEDED")
        assertThat(request.issuedCouponId).isEqualTo(999L)
        verify { requestRepository.save(match { it.status == "SUCCEEDED" && it.issuedCouponId == 999L }) }
        verify { acknowledgment.acknowledge() }
    }

    @Test
    fun `이미_발급받은_회원이면_요청을_FAILED_DUPLICATE로_마킹한다`() {
        val request = CouponIssueRequestEntity(
            id = 1L,
            couponId = 10L,
            memberId = 20L,
            status = "PENDING",
            requestedAt = ZonedDateTime.now(),
        )
        val eventHandledRecorder = mockk<EventHandledRecorder>()
        val requestRepository = mockk<CouponIssueRequestJpaRepository>(relaxed = true)
        val couponRepository = mockk<CouponJpaRepository>(relaxed = true)
        val issuedCouponRepository = mockk<IssuedCouponJpaRepository>()
        val acknowledgment = mockk<Acknowledgment>(relaxed = true)

        every { eventHandledRecorder.markHandled(any(), any()) } returns true
        every { requestRepository.findById(1L) } returns Optional.of(request)
        every { requestRepository.save(any()) } answers { firstArg() }
        every { issuedCouponRepository.existsByCouponIdAndMemberId(10L, 20L) } returns true

        val consumer = CouponIssueRequestConsumer(
            objectMapper = objectMapper,
            eventHandledRecorder = eventHandledRecorder,
            couponIssueRequestJpaRepository = requestRepository,
            couponJpaRepository = couponRepository,
            issuedCouponJpaRepository = issuedCouponRepository,
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

        assertThat(request.status).isEqualTo("FAILED_DUPLICATE")
        verify { requestRepository.save(match { it.status == "FAILED_DUPLICATE" }) }
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
