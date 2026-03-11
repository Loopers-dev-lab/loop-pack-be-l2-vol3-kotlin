package com.loopers.domain.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface IssuedCouponRepository {
    fun save(issuedCoupon: IssuedCoupon): IssuedCoupon
    fun findById(id: Long): IssuedCoupon?
    fun findByIdForUpdate(id: Long): IssuedCoupon?
    fun findAllByMemberId(memberId: Long): List<IssuedCoupon>
    fun existsByCouponIdAndMemberId(couponId: Long, memberId: Long): Boolean
    fun findAllByCouponId(couponId: Long, pageable: Pageable): Page<IssuedCoupon>
}
