package com.loopers.domain.coupon.repository

import com.loopers.domain.PageResult
import com.loopers.domain.coupon.model.Coupon

interface CouponRepository {
    fun save(coupon: Coupon): Coupon
    fun findById(id: Long): Coupon?
    fun findByIdForUpdate(id: Long): Coupon?
    fun findAll(page: Int, size: Int): PageResult<Coupon>
    fun findAllIncludeDeleted(page: Int, size: Int): PageResult<Coupon>
}
