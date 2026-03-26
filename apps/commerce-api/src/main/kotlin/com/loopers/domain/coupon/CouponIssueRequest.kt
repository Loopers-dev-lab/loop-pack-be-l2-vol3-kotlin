package com.loopers.domain.coupon

import java.time.ZonedDateTime

class CouponIssueRequest private constructor(
    val persistenceId: Long?,
    val requestId: String,
    val refCouponId: Long,
    val refUserId: Long,
    val status: CouponIssueRequestStatus,
    val failureReason: String?,
    val processedAt: ZonedDateTime?,
) {
    fun markSuccess(): CouponIssueRequest = CouponIssueRequest(
        persistenceId = persistenceId,
        requestId = requestId,
        refCouponId = refCouponId,
        refUserId = refUserId,
        status = CouponIssueRequestStatus.SUCCESS,
        failureReason = null,
        processedAt = ZonedDateTime.now(),
    )

    fun markFailed(reason: String): CouponIssueRequest = CouponIssueRequest(
        persistenceId = persistenceId,
        requestId = requestId,
        refCouponId = refCouponId,
        refUserId = refUserId,
        status = CouponIssueRequestStatus.FAILED,
        failureReason = reason,
        processedAt = ZonedDateTime.now(),
    )

    companion object {
        fun create(
            requestId: String,
            couponId: Long,
            userId: Long,
        ): CouponIssueRequest = CouponIssueRequest(
            persistenceId = null,
            requestId = requestId,
            refCouponId = couponId,
            refUserId = userId,
            status = CouponIssueRequestStatus.PENDING,
            failureReason = null,
            processedAt = null,
        )

        fun reconstitute(
            persistenceId: Long,
            requestId: String,
            refCouponId: Long,
            refUserId: Long,
            status: CouponIssueRequestStatus,
            failureReason: String?,
            processedAt: ZonedDateTime?,
        ): CouponIssueRequest = CouponIssueRequest(
            persistenceId = persistenceId,
            requestId = requestId,
            refCouponId = refCouponId,
            refUserId = refUserId,
            status = status,
            failureReason = failureReason,
            processedAt = processedAt,
        )
    }
}
