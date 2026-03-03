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
    ) {
        init {
            require(
                name != null ||
                    type != null ||
                    value != null ||
                    maxDiscount != null ||
                    minOrderAmount != null ||
                    totalQuantity != null ||
                    expiredAt != null,
            ) {
                "수정할 항목이 최소 하나 이상 있어야 합니다."
            }
        }
    }
}
