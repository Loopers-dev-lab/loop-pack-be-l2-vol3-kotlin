package com.loopers.domain.coupon

import java.math.BigDecimal
import java.math.RoundingMode

enum class CouponType {
    FIXED {
        override fun calculateDiscount(originalPrice: BigDecimal, value: BigDecimal): BigDecimal {
            return value.coerceAtMost(originalPrice)
        }
    },
    RATE {
        override fun calculateDiscount(originalPrice: BigDecimal, percentValue: BigDecimal): BigDecimal {
            val discountRate = (percentValue.divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)).coerceAtMost(BigDecimal.ONE)
            return (originalPrice * discountRate).setScale(0, RoundingMode.DOWN)
        }
    },
    ;

    abstract fun calculateDiscount(originalPrice: BigDecimal, value: BigDecimal): BigDecimal
}
