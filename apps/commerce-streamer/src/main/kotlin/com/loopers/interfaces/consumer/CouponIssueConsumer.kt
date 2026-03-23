package com.loopers.interfaces.consumer

import com.loopers.application.coupon.ProcessCouponIssueUseCase
import com.loopers.config.kafka.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
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
    fun consume(message: ConsumerRecord<Any, Any>) {
        val payload = message.value() as? Map<*, *>
            ?: throw IllegalArgumentException("페이로드 파싱 실패: offset=${message.offset()}")
        val eventId = payload["eventId"] as? String
            ?: throw IllegalArgumentException("eventId 누락: offset=${message.offset()}")
        val eventType = payload["eventType"] as? String
            ?: throw IllegalArgumentException("eventType 누락: offset=${message.offset()}")
        val couponId = (payload["couponId"] as? Number)?.toLong()
            ?: throw IllegalArgumentException("couponId 누락: offset=${message.offset()}")
        val userId = (payload["userId"] as? Number)?.toLong()
            ?: throw IllegalArgumentException("userId 누락: offset=${message.offset()}")

        if (eventType != COUPON_ISSUE_REQUESTED) {
            log.warn("알 수 없는 coupon 이벤트 타입: eventType={}", eventType)
            return
        }

        processCouponIssueUseCase.execute(
            eventId = eventId,
            couponId = couponId,
            userId = userId,
        )
    }

    companion object {
        const val TOPIC = "coupon-issue-requests"
        const val COUPON_ISSUE_REQUESTED = "COUPON_ISSUE_REQUESTED"
    }
}
