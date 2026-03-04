package com.loopers.domain.coupon

import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult

interface CouponRepository {
    fun save(coupon: Coupon): Coupon
    fun findById(id: Long): Coupon?
    fun findByIdWithLock(id: Long): Coupon?
    fun findByIdIn(ids: List<Long>): List<Coupon>
    fun findAll(pageQuery: PageQuery): PageResult<Coupon>
}
