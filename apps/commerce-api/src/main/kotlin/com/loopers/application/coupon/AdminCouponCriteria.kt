package com.loopers.application.coupon

import com.loopers.domain.coupon.DiscountType
import java.time.ZonedDateTime

data class RegisterCouponCriteria(
    val name: String,
    val discountType: DiscountType,
    val discountValue: Int,
    val totalQuantity: Int,
    val expiredAt: ZonedDateTime,
)

data class ModifyCouponCriteria(
    val couponId: Long,
    val name: String,
    val discountType: DiscountType,
    val discountValue: Int,
    val totalQuantity: Int,
    val expiredAt: ZonedDateTime,
)

data class ListCouponsCriteria(
    val page: Int,
    val size: Int,
)

data class ListIssuedCouponsCriteria(
    val couponId: Long,
    val page: Int,
    val size: Int,
)
