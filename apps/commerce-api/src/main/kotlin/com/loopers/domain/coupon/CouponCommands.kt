package com.loopers.domain.coupon

import java.time.ZonedDateTime

data class RegisterCouponCommand(
    val name: String,
    val discountType: DiscountType,
    val discountValue: Int,
    val totalQuantity: Int,
    val expiredAt: ZonedDateTime,
)

data class ModifyCouponCommand(
    val name: String,
    val discountType: DiscountType,
    val discountValue: Int,
    val totalQuantity: Int,
    val expiredAt: ZonedDateTime,
)

data class IssueCouponCommand(
    val couponId: Long,
    val userId: Long,
)
