package com.loopers.domain.coupon

import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse

interface CouponRepository {
    fun save(coupon: Coupon): Coupon

    fun findById(id: Long): Coupon?

    fun findAll(pageRequest: PageRequest): PageResponse<Coupon>
}
