package com.loopers.domain.coupon.strategy

import java.math.BigDecimal
import java.math.RoundingMode

class RateCouponStrategy(private val rate: BigDecimal) : CouponStrategy {
    override fun calculateDiscount(orderAmount: BigDecimal): BigDecimal {
        val discountRate = rate.divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
            .coerceAtMost(BigDecimal.ONE)
        return (orderAmount * discountRate).setScale(0, RoundingMode.DOWN)
    }
}
