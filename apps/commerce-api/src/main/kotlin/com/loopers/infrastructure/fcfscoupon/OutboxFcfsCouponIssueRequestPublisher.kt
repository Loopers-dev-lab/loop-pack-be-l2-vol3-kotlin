package com.loopers.infrastructure.fcfscoupon

import com.loopers.application.fcfscoupon.FcfsCouponIssueRequestPublisher
import com.loopers.application.outbox.OutboxEventPublisher
import com.loopers.event.EventContract
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OutboxFcfsCouponIssueRequestPublisher(
    private val outboxEventPublisher: OutboxEventPublisher,
) : FcfsCouponIssueRequestPublisher {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun publish(requestId: Long, templateId: Long, memberId: Long) {
        outboxEventPublisher.publish(
            aggregateType = EventContract.AGGREGATE_FCFS_COUPON,
            aggregateId = requestId.toString(),
            eventType = EventContract.EVENT_FCFS_COUPON_ISSUE_REQUESTED,
            payload = mapOf(
                "requestId" to requestId,
                "templateId" to templateId,
                "memberId" to memberId,
            ),
            partitionKey = templateId.toString(),
            topic = EventContract.COUPON_ISSUE_REQUEST_TOPIC,
        )
        log.info("Outbox 선착순 쿠폰 발급 요청 발행: requestId={}, templateId={}, memberId={}", requestId, templateId, memberId)
    }
}
