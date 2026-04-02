package com.loopers.application.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.config.kafka.event.CouponIssueRequestMessage
import com.loopers.domain.coupon.CouponIssueRequestModel
import com.loopers.domain.outbox.OutboxEventModel
import com.loopers.infrastructure.outbox.OutboxEventJpaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.util.UUID

@Component
class CouponIssueRequestOutboxAppender(
    private val outboxEventJpaRepository: OutboxEventJpaRepository,
    private val objectMapper: ObjectMapper,
    @Value("\${step3.kafka.coupon-issue-request-topic}") private val couponIssueRequestTopic: String,
) {
    fun append(request: CouponIssueRequestModel) {
        val message = CouponIssueRequestMessage(
            eventId = UUID.randomUUID().toString(),
            requestId = request.id,
            couponId = request.couponId,
            userId = request.userId,
            requestedAt = ZonedDateTime.now(),
        )

        outboxEventJpaRepository.save(
            OutboxEventModel(
                eventId = message.eventId,
                topic = couponIssueRequestTopic,
                partitionKey = partitionKey(request.couponId),
                payload = objectMapper.writeValueAsString(message),
            ),
        )
    }

    private fun partitionKey(couponId: Long): String = "coupon:$couponId"
}
