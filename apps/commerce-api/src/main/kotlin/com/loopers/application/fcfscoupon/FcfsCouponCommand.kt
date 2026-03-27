package com.loopers.application.fcfscoupon

import com.loopers.domain.coupon.CouponType
import java.time.ZonedDateTime

class FcfsCouponCommand {
    data class CreateTemplate(
        val name: String,
        val description: String?,
        val discountType: CouponType,
        val discountValue: Long,
        val minOrderAmount: Long?,
        val maxDiscountAmount: Long?,
        val totalQuantity: Int,
        val startedAt: ZonedDateTime,
        val endedAt: ZonedDateTime,
    )

    data class UpdateTemplate(
        val name: String,
        val description: String?,
        val discountType: CouponType,
        val discountValue: Long,
        val minOrderAmount: Long?,
        val maxDiscountAmount: Long?,
        val totalQuantity: Int,
        val startedAt: ZonedDateTime,
        val endedAt: ZonedDateTime,
    )
}
