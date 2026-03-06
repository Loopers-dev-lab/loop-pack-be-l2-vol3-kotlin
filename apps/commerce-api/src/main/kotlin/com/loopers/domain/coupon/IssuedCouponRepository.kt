package com.loopers.domain.coupon

import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse

interface IssuedCouponRepository {
    fun save(issuedCoupon: IssuedCoupon): IssuedCoupon

    fun use(issuedCoupon: IssuedCoupon)

    fun findById(id: Long): IssuedCoupon?

    fun findAllByUserId(userId: Long): List<IssuedCoupon>

    fun findAllByCouponId(couponId: Long, pageRequest: PageRequest): PageResponse<IssuedCoupon>
}
