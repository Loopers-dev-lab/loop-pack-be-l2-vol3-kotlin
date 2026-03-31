package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponIssueStatusResult
import com.loopers.application.coupon.RequestCouponIssueResult
import com.loopers.application.coupon.UserIssuedCouponResult
import com.loopers.application.coupon.UserListCouponsResult
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.DiscountType
import java.time.ZonedDateTime

class CouponV1Dto {
    data class IssuedCouponResponse(
        val id: Long,
        val couponId: Long,
        val discountType: DiscountType,
        val discountValue: Int,
        val status: CouponStatus,
        val expiredAt: ZonedDateTime,
        val usedAt: ZonedDateTime?,
    ) {
        companion object {
            fun from(result: UserIssuedCouponResult): IssuedCouponResponse {
                return IssuedCouponResponse(
                    id = result.id,
                    couponId = result.couponId,
                    discountType = result.discountType,
                    discountValue = result.discountValue,
                    status = result.status,
                    expiredAt = result.expiredAt,
                    usedAt = result.usedAt,
                )
            }
        }
    }

    data class IssuedCouponsResponse(
        val content: List<IssuedCouponResponse>,
        val page: Int,
        val size: Int,
        val hasNext: Boolean,
    ) {
        companion object {
            fun from(result: UserListCouponsResult): IssuedCouponsResponse {
                return IssuedCouponsResponse(
                    content = result.content.map { IssuedCouponResponse.from(it) },
                    page = result.page,
                    size = result.size,
                    hasNext = result.hasNext,
                )
            }
        }
    }

    data class CouponIssueRequestResponse(
        val requestId: Long,
        val status: String,
    ) {
        companion object {
            fun from(result: RequestCouponIssueResult): CouponIssueRequestResponse {
                return CouponIssueRequestResponse(
                    requestId = result.requestId,
                    status = result.status.name,
                )
            }
        }
    }

    data class CouponIssueStatusResponse(
        val requestId: Long,
        val status: String,
        val failureReason: String?,
    ) {
        companion object {
            fun from(result: CouponIssueStatusResult): CouponIssueStatusResponse {
                return CouponIssueStatusResponse(
                    requestId = result.requestId,
                    status = result.status.name,
                    failureReason = result.failureReason,
                )
            }
        }
    }
}
