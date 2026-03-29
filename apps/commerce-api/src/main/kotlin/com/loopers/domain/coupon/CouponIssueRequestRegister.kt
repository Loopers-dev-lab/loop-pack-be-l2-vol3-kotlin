package com.loopers.domain.coupon

import org.springframework.stereotype.Component

@Component
class CouponIssueRequestRegister(
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
) {
    fun register(couponId: Long, memberId: Long): CouponIssueRequest {
        return couponIssueRequestRepository.save(
            CouponIssueRequest(
                couponId = couponId,
                memberId = memberId,
            ),
        )
    }
}
