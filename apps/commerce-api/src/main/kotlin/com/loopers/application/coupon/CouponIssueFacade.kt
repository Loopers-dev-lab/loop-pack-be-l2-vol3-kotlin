package com.loopers.application.coupon

import com.loopers.application.outbox.OutboxPublisher
import com.loopers.domain.coupon.CouponService
import com.loopers.event.payload.CouponIssueRequestPayload
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class CouponIssueFacade(
    private val couponService: CouponService,
    private val outboxPublisher: OutboxPublisher,
) {

    @Transactional
    fun issueAsync(couponId: Long, userId: Long): String {
        couponService.reserveAsyncIssue(couponId, userId)

        val requestId = UUID.randomUUID().toString()
        couponService.saveIssueRequest(requestId, couponId, userId)

        outboxPublisher.publish(
            aggregateType = "COUPON",
            aggregateId = couponId.toString(),
            eventType = "COUPON_ISSUE_REQUESTED",
            version = System.currentTimeMillis(),
            payload = CouponIssueRequestPayload(
                couponId = couponId,
                userId = userId,
                requestId = requestId,
            ),
        )

        return requestId
    }

    @Transactional(readOnly = true)
    fun getIssueRequest(requestId: String): CouponIssueRequestInfo {
        val issueRequest = couponService.findIssueRequestByRequestId(requestId)
        return CouponIssueRequestInfo.from(issueRequest)
    }
}
