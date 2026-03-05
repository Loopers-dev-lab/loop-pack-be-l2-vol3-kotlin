package com.loopers.domain.coupon

import java.math.BigDecimal
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
    val status: CouponStatus,
    val usedAt: ZonedDateTime?,
) {
    companion object {
        fun from(model: IssuedCouponModel): IssuedCouponInfo {
            return IssuedCouponInfo(
                id = model.id,
                couponId = model.couponId,
                userId = model.userId,
                status = model.status,
                usedAt = model.usedAt,
            )
        }
    }
}

data class CouponDiscountInfo(
    val couponId: Long,
    val discountType: DiscountType,
    val discountValue: Int,
) {
    fun calculateDiscount(originalPrice: BigDecimal): BigDecimal {
        return when (discountType) {
            DiscountType.FIXED -> originalPrice.min(BigDecimal(discountValue))
            DiscountType.PERCENTAGE -> originalPrice.multiply(BigDecimal(discountValue))
                .divide(BigDecimal(100), 2, java.math.RoundingMode.FLOOR)
        }
    }
}
