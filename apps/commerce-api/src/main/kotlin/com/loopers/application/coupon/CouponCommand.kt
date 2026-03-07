package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.ExpirationPolicy
import java.time.ZonedDateTime

class CouponCommand {
    data class CreateTemplate(
        val name: String,
        val type: CouponType,
        val value: Long,
        val minOrderAmount: Long?,
        val maxDiscountAmount: Long?,
        val expirationPolicy: ExpirationPolicy,
        val expiredAt: ZonedDateTime?,
        val validDays: Int?,
    )

    data class UpdateTemplate(
        val name: String,
        val type: CouponType,
        val value: Long,
        val minOrderAmount: Long?,
        val maxDiscountAmount: Long?,
        val expirationPolicy: ExpirationPolicy,
        val expiredAt: ZonedDateTime?,
        val validDays: Int?,
    )
}
