package com.loopers.application.coupon

import java.math.BigDecimal

object CouponCommand {
    data class CreateCoupon(
        val name: String,
        val type: String,
        val value: Long,
        val maxDiscount: BigDecimal?,
        val minOrderAmount: BigDecimal?,
        val totalQuantity: Int?,
        val expiredAt: String,
    )

    data class UpdateCoupon(
        val name: String?,
        val type: String?,
        val value: Long?,
        val maxDiscount: BigDecimal?,
        val minOrderAmount: BigDecimal?,
        val totalQuantity: Int?,
        val expiredAt: String?,
    )
}
