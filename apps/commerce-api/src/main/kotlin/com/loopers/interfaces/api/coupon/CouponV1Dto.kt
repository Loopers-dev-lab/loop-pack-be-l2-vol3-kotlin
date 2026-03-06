package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponInfo

class CouponV1Dto {

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
