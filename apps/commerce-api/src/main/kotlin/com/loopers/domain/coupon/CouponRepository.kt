package com.loopers.domain.coupon

import com.loopers.support.PageResult

interface CouponRepository {
    fun findByIdOrNull(id: Long): Coupon?
    fun findActiveByIdOrNull(id: Long): Coupon?
    fun findAllActive(page: Int, size: Int): PageResult<Coupon>
    fun save(coupon: Coupon): Coupon
}
