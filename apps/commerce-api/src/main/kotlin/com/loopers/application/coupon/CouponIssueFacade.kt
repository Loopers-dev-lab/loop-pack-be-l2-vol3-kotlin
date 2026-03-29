package com.loopers.application.coupon

import com.loopers.application.outbox.OutboxPublisher
import com.loopers.domain.coupon.CouponIssueRepository
import com.loopers.domain.coupon.CouponService
import com.loopers.event.AggregateTypes
import com.loopers.event.EventTypes
import com.loopers.event.payload.CouponIssueRequestPayload
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class CouponIssueFacade(
    private val couponService: CouponService,
    private val couponIssueRepository: CouponIssueRepository,
    private val outboxPublisher: OutboxPublisher,
) {

    @Transactional
    fun issueAsync(couponId: Long, userId: Long): String {
        couponService.reserveAsyncIssue(couponId, userId)

        try {
            val requestId = UUID.randomUUID().toString()
            couponService.saveIssueRequest(requestId, couponId, userId)

            outboxPublisher.publish(
                aggregateType = AggregateTypes.COUPON,
                aggregateId = couponId.toString(),
                eventType = EventTypes.COUPON_ISSUE_REQUESTED,
                version = System.currentTimeMillis(),
                payload = CouponIssueRequestPayload(
                    couponId = couponId,
                    userId = userId,
                    requestId = requestId,
                ),
            )

            return requestId
        } catch (e: Exception) {
            couponIssueRepository.restore(couponId, userId)
            throw e
        }
    }

    @Transactional(readOnly = true)
    fun getIssueRequest(requestId: String, userId: Long): CouponIssueRequestInfo {
        val issueRequest = couponService.findIssueRequestByRequestIdAndUserId(requestId, userId)
        return CouponIssueRequestInfo.from(issueRequest)
    }
}
