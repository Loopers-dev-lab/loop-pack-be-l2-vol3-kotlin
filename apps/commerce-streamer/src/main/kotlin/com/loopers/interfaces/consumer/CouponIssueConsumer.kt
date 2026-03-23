package com.loopers.interfaces.consumer

import com.loopers.application.coupon.ProcessCouponIssueUseCase
import com.loopers.config.kafka.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class CouponIssueConsumer(
    private val processCouponIssueUseCase: ProcessCouponIssueUseCase,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [TOPIC],
        containerFactory = KafkaConfig.BATCH_LISTENER,
    )
    fun consume(
        messages: List<ConsumerRecord<Any, Any>>,
        acknowledgment: Acknowledgment,
    ) {
        for (message in messages) {
            try {
                val payload = message.value() as? Map<*, *> ?: continue
                val eventId = payload["eventId"] as? String ?: continue
                val eventType = payload["eventType"] as? String ?: continue
                val couponId = (payload["couponId"] as? Number)?.toLong() ?: continue
                val userId = (payload["userId"] as? Number)?.toLong() ?: continue

                if (eventType != COUPON_ISSUE_REQUESTED) {
                    log.warn("알 수 없는 coupon 이벤트 타입: eventType={}", eventType)
                    continue
                }

                processCouponIssueUseCase.execute(
                    eventId = eventId,
                    couponId = couponId,
                    userId = userId,
                )
            } catch (ex: Exception) {
                log.error("coupon-issue-requests 처리 실패: offset={}", message.offset(), ex)
            }
        }
        acknowledgment.acknowledge()
    }

    companion object {
        const val TOPIC = "coupon-issue-requests"
        const val COUPON_ISSUE_REQUESTED = "COUPON_ISSUE_REQUESTED"
    }
}
