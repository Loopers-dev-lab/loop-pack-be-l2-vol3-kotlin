package com.loopers.application.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.math.BigDecimal
import java.time.ZonedDateTime

class CouponCommand {
    data class CreateCoupon(
        val name: String,
        val type: String,
        val value: Long,
        val maxDiscount: BigDecimal?,
        val minOrderAmount: BigDecimal?,
        val totalQuantity: Int?,
        val expiredAt: ZonedDateTime,
    )

    data class UpdateCoupon(
        val name: String?,
        val type: String?,
        val value: Long?,
        val maxDiscount: BigDecimal?,
        val minOrderAmount: BigDecimal?,
        val totalQuantity: Int?,
        val expiredAt: ZonedDateTime?,
    ) {
        init {
            val hasAnyField = name != null || type != null || value != null ||
                maxDiscount != null || minOrderAmount != null || totalQuantity != null || expiredAt != null
            if (!hasAnyField) {
                throw CoreException(ErrorType.BAD_REQUEST, "수정할 항목이 최소 하나 이상 있어야 합니다.")
            }
        }
    }
}
