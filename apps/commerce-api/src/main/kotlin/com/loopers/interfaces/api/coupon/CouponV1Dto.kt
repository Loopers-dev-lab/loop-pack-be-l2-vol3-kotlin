package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponInfo

class CouponV1Dto {

    data class IssueRequestResponse(
        val requestId: Long,
        val couponId: Long,
        val memberId: Long,
        val status: String,
        val issuedCouponId: Long?,
        val failureReason: String?,
        val requestedAt: String,
    ) {
        companion object {
            fun from(info: CouponInfo.IssueRequestDetail) = IssueRequestResponse(
                requestId = info.requestId,
                couponId = info.couponId,
                memberId = info.memberId,
                status = info.status,
                issuedCouponId = info.issuedCouponId,
                failureReason = info.failureReason,
                requestedAt = info.requestedAt,
            )
        }
    }

    data class IssuedDetailResponse(
        val id: Long,
        val couponName: String,
        val couponType: String,
        val discountValue: Long,
        val status: String,
        val issuedAt: String,
        val expiredAt: String,
    ) {
        companion object {
            fun from(info: CouponInfo.IssuedDetail) = IssuedDetailResponse(
                id = info.id,
                couponName = info.couponName,
                couponType = info.couponType,
                discountValue = info.discountValue,
                status = info.status,
                issuedAt = info.issuedAt,
                expiredAt = info.expiredAt,
            )
        }
    }
}
