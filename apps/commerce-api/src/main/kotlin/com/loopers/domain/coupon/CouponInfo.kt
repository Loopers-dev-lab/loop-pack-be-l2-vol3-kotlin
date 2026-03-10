package com.loopers.domain.coupon

import java.time.ZonedDateTime

data class CouponInfo(
    val id: Long,
    val name: String,
    val discountType: DiscountType,
    val discountValue: Int,
    val totalQuantity: Int,
    val issuedQuantity: Int,
    val expiredAt: ZonedDateTime,
) {
    companion object {
        fun from(model: CouponModel): CouponInfo {
            return CouponInfo(
                id = model.id,
                name = model.name,
                discountType = model.discountType,
                discountValue = model.discountValue,
                totalQuantity = model.totalQuantity,
                issuedQuantity = model.issuedQuantity,
                expiredAt = model.expiredAt,
            )
        }
    }
}

data class IssuedCouponInfo(
    val id: Long,
    val couponId: Long,
    val userId: Long,
    val discountType: DiscountType,
    val discountValue: Int,
    val status: CouponStatus,
    val expiredAt: ZonedDateTime,
    val usedAt: ZonedDateTime?,
) {
    companion object {
        fun from(model: IssuedCouponModel): IssuedCouponInfo {
            return IssuedCouponInfo(
                id = model.id,
                couponId = model.couponId,
                userId = model.userId,
                discountType = model.discountType,
                discountValue = model.discountValue,
                status = model.status,
                expiredAt = model.expiredAt,
                usedAt = model.usedAt,
            )
        }
    }
}

data class CouponDiscountInfo(
    val discountType: DiscountType,
    val discountValue: Int,
)
