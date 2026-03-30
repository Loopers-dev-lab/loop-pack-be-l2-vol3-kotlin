package com.loopers.interfaces.consumer

import com.loopers.config.kafka.KafkaConfig
import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.event.CouponIssueResultMessage
import com.loopers.event.KafkaTopics
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CouponIssueResultConsumer(
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopics.COUPON_ISSUE_RESULTS],
        groupId = "coupon-result-consumer",
        containerFactory = KafkaConfig.RECORD_LISTENER,
    )
    @Transactional
    fun consume(message: CouponIssueResultMessage, acknowledgment: Acknowledgment) {
        try {
            val request = couponIssueRequestRepository.findByRequestId(message.requestId)
            if (request == null) {
                log.warn("발급 요청을 찾을 수 없음: requestId=${message.requestId}")
                acknowledgment.acknowledge()
                return
            }

            when (message.status) {
                "SUCCESS" -> request.markSuccess()
                "FAILED" -> request.markFailed(message.failureReason)
                else -> log.warn("알 수 없는 상태: ${message.status}")
            }

            couponIssueRequestRepository.save(request)
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            log.error("쿠폰 발급 결과 처리 실패: requestId=${message.requestId}", e)
        }
    }
}
