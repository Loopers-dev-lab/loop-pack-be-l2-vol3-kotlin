package com.loopers.domain.coupon.strategy

import java.math.BigDecimal

interface CouponStrategy {
    fun calculateDiscount(orderAmount: BigDecimal): BigDecimal
}
