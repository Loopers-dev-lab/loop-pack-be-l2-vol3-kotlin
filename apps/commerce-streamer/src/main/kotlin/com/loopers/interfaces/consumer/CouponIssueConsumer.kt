package com.loopers.interfaces.consumer

import com.loopers.application.coupon.ProcessCouponIssueUseCase
import com.loopers.config.kafka.KafkaConfig
import com.loopers.interfaces.consumer.dto.CouponIssuePayload
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class CouponIssueConsumer(
    private val processCouponIssueUseCase: ProcessCouponIssueUseCase,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [TOPIC],
        containerFactory = KafkaConfig.RECORD_LISTENER,
    )
    fun consume(payload: CouponIssuePayload) {
        if (payload.eventType != COUPON_ISSUE_REQUESTED) {
            log.warn("알 수 없는 coupon 이벤트 타입: eventType={}", payload.eventType)
            return
        }

        processCouponIssueUseCase.execute(
            eventId = payload.eventId,
            couponId = payload.couponId,
            userId = payload.userId,
        )
    }

    companion object {
        const val TOPIC = "coupon-issue-requests"
        const val COUPON_ISSUE_REQUESTED = "COUPON_ISSUE_REQUESTED"
    }
}
