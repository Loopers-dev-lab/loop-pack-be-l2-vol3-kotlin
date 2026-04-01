package com.loopers.application.coupon

import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.common.vo.UserId
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
        val couponIdVo = CouponId(couponId)
        val userIdVo = UserId(userId)

        val request = CouponIssueRequest(
            requestId = requestId,
            couponId = couponIdVo,
            userId = userIdVo,
        )
        couponIssueRequestRepository.save(request)

        val outbox = CouponOutbox(
            eventId = requestId,
            eventType = CouponOutbox.CouponOutboxEventType.COUPON_ISSUE_REQUESTED,
            couponId = couponIdVo,
            userId = userIdVo,
        )
        couponOutboxRepository.save(outbox)

        return CouponIssueRequestInfo(requestId = requestId)
    }
}
