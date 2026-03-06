package com.loopers.domain.coupon.strategy

import java.math.BigDecimal

class FixedCouponStrategy(private val value: BigDecimal) : CouponStrategy {
    override fun calculateDiscount(orderAmount: BigDecimal): BigDecimal {
        return value.coerceAtMost(orderAmount)
    }
}
