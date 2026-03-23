package com.loopers.domain.coupon.repository

import com.loopers.domain.coupon.model.Coupon

interface CouponRepository {
    fun findById(id: Long): Coupon?
    fun save(coupon: Coupon): Coupon
}
