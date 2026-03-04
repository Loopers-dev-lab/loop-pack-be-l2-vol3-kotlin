package com.loopers.domain.coupon

interface CouponRepository {
    fun save(coupon: Coupon): Long
    fun findById(id: Long): Coupon?
    fun incrementIssuedCount(id: Long): Int
    fun findAll(): List<Coupon>
}
