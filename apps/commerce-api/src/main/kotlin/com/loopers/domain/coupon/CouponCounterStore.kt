package com.loopers.domain.coupon

interface CouponCounterStore {
    fun increment(couponId: Long): Long
    fun decrement(couponId: Long)
}
