package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.consumer.EventHandledRecorder
import com.loopers.application.consumer.RawIntegrationEvent
import com.loopers.config.kafka.KafkaConfig
import com.loopers.infrastructure.coupon.CouponIssueRequestEntity
import com.loopers.infrastructure.coupon.CouponIssueRequestJpaRepository
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.coupon.IssuedCouponEntity
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import com.loopers.kafka.CouponIssueRequestedPayload
import com.loopers.kafka.KafkaTopics
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class CouponIssueRequestConsumer(
    private val objectMapper: ObjectMapper,
    private val eventHandledRecorder: EventHandledRecorder,
    private val couponIssueRequestJpaRepository: CouponIssueRequestJpaRepository,
    private val couponJpaRepository: CouponJpaRepository,
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
) {
    companion object {
        private const val CONSUMER_GROUP = "coupon-issue-request-consumer"
    }

    @Transactional
    @KafkaListener(
        topics = [KafkaTopics.COUPON_ISSUE_REQUESTS],
        groupId = CONSUMER_GROUP,
        containerFactory = KafkaConfig.MANUAL_LISTENER,
    )
    fun consume(
        message: String,
        acknowledgment: Acknowledgment,
    ) {
        val event = objectMapper.readValue(message, RawIntegrationEvent::class.java)
        if (!eventHandledRecorder.markHandled(CONSUMER_GROUP, event.eventId)) {
            acknowledgment.acknowledge()
            return
        }

        if (event.eventType != "CouponIssueRequested") {
            acknowledgment.acknowledge()
            return
        }

        val payload = objectMapper.treeToValue(event.payload, CouponIssueRequestedPayload::class.java)
        val request = couponIssueRequestJpaRepository.findById(payload.requestId)
            .orElse(null)
            ?: run {
                acknowledgment.acknowledge()
                return
            }

        if (request.status != "PENDING") {
            acknowledgment.acknowledge()
            return
        }

        if (issuedCouponJpaRepository.existsByCouponIdAndMemberId(payload.couponId, payload.memberId)) {
            request.markFailed("FAILED_DUPLICATE", "이미 발급받은 쿠폰입니다.")
            couponIssueRequestJpaRepository.save(request)
            acknowledgment.acknowledge()
            return
        }

        if (couponJpaRepository.tryIncreaseIssuedCount(payload.couponId) == 0) {
            request.markFailed("FAILED_SOLD_OUT", "선착순 쿠폰이 모두 소진되었습니다.")
            couponIssueRequestJpaRepository.save(request)
            acknowledgment.acknowledge()
            return
        }

        val issuedCoupon = issuedCouponJpaRepository.save(
            IssuedCouponEntity(
                couponId = payload.couponId,
                memberId = payload.memberId,
                status = "AVAILABLE",
                issuedAt = ZonedDateTime.now(),
            ),
        )
        request.status = "SUCCEEDED"
        request.issuedCouponId = issuedCoupon.id
        request.failureReason = null
        couponIssueRequestJpaRepository.save(request)
        acknowledgment.acknowledge()
    }

    private fun CouponIssueRequestEntity.markFailed(status: String, reason: String) {
        this.status = status
        this.failureReason = reason
        this.issuedCouponId = null
    }
}
