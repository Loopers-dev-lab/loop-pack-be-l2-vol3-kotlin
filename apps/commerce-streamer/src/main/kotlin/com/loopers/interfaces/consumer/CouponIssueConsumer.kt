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
        var failCount = 0
        for (message in messages) {
            var eventId: String? = null
            try {
                val payload = message.value() as? Map<*, *>
                if (payload == null) {
                    failCount++
                    log.warn("페이로드 파싱 실패: offset={}", message.offset())
                    continue
                }

                eventId = payload["eventId"] as? String
                val eventType = payload["eventType"] as? String
                val couponId = (payload["couponId"] as? Number)?.toLong()
                val userId = (payload["userId"] as? Number)?.toLong()

                if (eventId == null || eventType == null || couponId == null || userId == null) {
                    failCount++
                    log.warn(
                        "필수 필드 누락: offset={}, eventId={}, eventType={}, couponId={}, userId={}",
                        message.offset(),
                        eventId,
                        eventType,
                        couponId,
                        userId,
                    )
                    continue
                }

                if (eventType != COUPON_ISSUE_REQUESTED) {
                    failCount++
                    log.warn("알 수 없는 coupon 이벤트 타입: eventType={}", eventType)
                    continue
                }

                processCouponIssueUseCase.execute(
                    eventId = eventId,
                    couponId = couponId,
                    userId = userId,
                )
            } catch (ex: Exception) {
                failCount++
                log.error(
                    "coupon-issue-requests 처리 실패: offset={}, eventId={}",
                    message.offset(),
                    eventId,
                    ex,
                )
            }
        }
        if (failCount > 0) {
            log.warn("배치 처리 완료: 총 {}건 중 {}건 실패", messages.size, failCount)
        }
        acknowledgment.acknowledge()
    }

    companion object {
        const val TOPIC = "coupon-issue-requests"
        const val COUPON_ISSUE_REQUESTED = "COUPON_ISSUE_REQUESTED"
    }
}
