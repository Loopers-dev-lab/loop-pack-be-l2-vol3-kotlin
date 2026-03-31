package com.loopers.application.user.coupon

class UserCouponCommand {
    data class Issue(
        val userId: Long,
        val couponId: Long,
    )

    data class IssueRequestStatus(
        val userId: Long,
        val requestId: Long,
    )
}
