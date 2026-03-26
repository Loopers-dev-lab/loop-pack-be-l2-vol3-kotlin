package com.loopers.interfaces.consumer

import com.loopers.application.coupon.CouponIssueService
import com.loopers.application.event.IdempotencyService
import com.loopers.config.kafka.KafkaConfig
import com.loopers.event.CouponIssueResultMessage
import com.loopers.event.KafkaEventMessage
import com.loopers.event.KafkaTopics
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class CouponIssueConsumer(
    private val idempotencyService: IdempotencyService,
    private val couponIssueService: CouponIssueService,
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopics.COUPON_ISSUE_REQUESTS],
        groupId = "coupon-issue-consumer",
        containerFactory = KafkaConfig.ORDERED_RECORD_LISTENER,
    )
    fun consume(message: KafkaEventMessage, acknowledgment: Acknowledgment) {
        try {
            if (idempotencyService.isAlreadyHandled(message.eventId)) {
                acknowledgment.acknowledge()
                return
            }

            val requestId = message.payload["requestId"]?.toString() ?: message.eventId
            val couponId = (message.payload["couponId"] as Number).toLong()
            val userId = (message.payload["userId"] as Number).toLong()

            val result = couponIssueService.issue(couponId, userId)

            val resultMessage = CouponIssueResultMessage(
                requestId = requestId,
                couponId = couponId,
                userId = userId,
                status = if (result.success) "SUCCESS" else "FAILED",
                failureReason = result.failureReason,
                processedAt = ZonedDateTime.now(),
            )
            kafkaTemplate.send(KafkaTopics.COUPON_ISSUE_RESULTS, couponId.toString(), resultMessage)

            idempotencyService.markHandled(
                eventId = message.eventId,
                aggregateType = message.aggregateType,
                aggregateId = message.aggregateId,
                eventType = message.eventType,
            )

            acknowledgment.acknowledge()
        } catch (e: Exception) {
            log.error("쿠폰 발급 이벤트 처리 실패: eventId=${message.eventId}", e)
        }
    }
}
