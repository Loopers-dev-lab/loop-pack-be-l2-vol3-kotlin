package com.loopers.application.user.coupon

interface UserCouponIssueRequestStatusUseCase {
    fun getStatus(command: UserCouponCommand.IssueRequestStatus): UserCouponResult.IssueRequestStatus
}
