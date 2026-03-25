package com.loopers.application.coupon

import com.loopers.domain.coupon.model.CouponIssueRequest
import com.loopers.domain.coupon.repository.CouponIssueRequestRepository
import com.loopers.domain.outbox.model.CouponOutbox
import com.loopers.domain.outbox.repository.CouponOutboxRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class RequestCouponIssueUseCase(
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
    private val couponOutboxRepository: CouponOutboxRepository,
) {

    @Transactional
    fun execute(userId: Long, couponId: Long): CouponIssueRequestInfo {
        val requestId = UUID.randomUUID().toString()

        val request = CouponIssueRequest(
            requestId = requestId,
            couponId = couponId,
            userId = userId,
        )
        couponIssueRequestRepository.save(request)

        val outbox = CouponOutbox(
            eventId = requestId,
            eventType = COUPON_ISSUE_REQUESTED,
            couponId = couponId,
            userId = userId,
        )
        couponOutboxRepository.save(outbox)

        return CouponIssueRequestInfo(requestId = requestId)
    }

    companion object {
        const val COUPON_ISSUE_REQUESTED = "COUPON_ISSUE_REQUESTED"
    }
}
