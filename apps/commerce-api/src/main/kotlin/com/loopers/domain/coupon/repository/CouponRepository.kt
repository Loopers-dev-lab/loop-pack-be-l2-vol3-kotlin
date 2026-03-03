package com.loopers.domain.coupon.repository

import com.loopers.domain.PageResult
import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.coupon.model.Coupon

interface CouponRepository {
    fun save(coupon: Coupon): Coupon
    fun findById(id: CouponId): Coupon?
    fun findByIdForUpdate(id: CouponId): Coupon?
    fun findAll(page: Int, size: Int): PageResult<Coupon>
    fun findAllIncludeDeleted(page: Int, size: Int): PageResult<Coupon>
    fun findAllByIds(ids: List<CouponId>): List<Coupon>
}
