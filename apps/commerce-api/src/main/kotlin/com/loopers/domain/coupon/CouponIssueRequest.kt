package com.loopers.domain.coupon

class CouponIssueRequest private constructor(
    val id: Long?,
    val couponId: Long,
    val userId: Long,
    val status: Status,
    val failureReasonCode: String?,
    val issuedCouponId: Long?,
) {
    enum class Status {
        REQUESTED,
        ISSUED,
        FAILED,
    }

    companion object {
        fun request(
            couponId: Long,
            userId: Long,
        ): CouponIssueRequest = CouponIssueRequest(
            id = null,
            couponId = couponId,
            userId = userId,
            status = Status.REQUESTED,
            failureReasonCode = null,
            issuedCouponId = null,
        )

        fun retrieve(
            id: Long,
            couponId: Long,
            userId: Long,
            status: Status,
            failureReasonCode: String?,
            issuedCouponId: Long?,
        ): CouponIssueRequest = CouponIssueRequest(
            id = id,
            couponId = couponId,
            userId = userId,
            status = status,
            failureReasonCode = failureReasonCode,
            issuedCouponId = issuedCouponId,
        )
    }

    fun markIssued(issuedCouponId: Long): CouponIssueRequest = CouponIssueRequest(
        id = id,
        couponId = couponId,
        userId = userId,
        status = Status.ISSUED,
        failureReasonCode = null,
        issuedCouponId = issuedCouponId,
    )

    fun markFailed(failureReasonCode: String): CouponIssueRequest = CouponIssueRequest(
        id = id,
        couponId = couponId,
        userId = userId,
        status = Status.FAILED,
        failureReasonCode = failureReasonCode,
        issuedCouponId = null,
    )
}
