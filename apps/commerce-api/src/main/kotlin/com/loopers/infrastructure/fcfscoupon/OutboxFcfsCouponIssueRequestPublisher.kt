package com.loopers.infrastructure.fcfscoupon

import com.loopers.application.fcfscoupon.FcfsCouponIssueRequestPublisher
import com.loopers.application.outbox.OutboxEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OutboxFcfsCouponIssueRequestPublisher(
    private val outboxEventPublisher: OutboxEventPublisher,
) : FcfsCouponIssueRequestPublisher {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun publish(requestId: Long, templateId: Long, memberId: Long) {
        outboxEventPublisher.publish(
            aggregateType = AGGREGATE_TYPE,
            aggregateId = requestId.toString(),
            eventType = EVENT_TYPE,
            payload = mapOf(
                "requestId" to requestId,
                "templateId" to templateId,
                "memberId" to memberId,
            ),
            partitionKey = templateId.toString(),
            topic = TOPIC,
        )
        log.info("Outbox 선착순 쿠폰 발급 요청 발행: requestId={}, templateId={}, memberId={}", requestId, templateId, memberId)
    }

    companion object {
        const val AGGREGATE_TYPE = "FCFS_COUPON"
        const val EVENT_TYPE = "FcfsCouponIssueRequested"
        const val TOPIC = "coupon.issue.request"
    }
}
