package com.loopers.domain.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface IssuedCouponRepository {
    fun save(issuedCoupon: IssuedCoupon): IssuedCoupon
    fun findById(id: Long): IssuedCoupon?
    fun findByIdWithLock(id: Long): IssuedCoupon?
    fun findAllByUserId(userId: Long): List<IssuedCoupon>
    fun findAllByCouponId(couponId: Long, pageable: Pageable): Page<IssuedCoupon>
    fun existsByCouponIdAndUserId(couponId: Long, userId: Long): Boolean
}
