package com.loopers.domain.coupon

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface IssuedCouponRepository {
    fun findById(id: Long): IssuedCouponModel?
    fun findByIdWithLock(id: Long): IssuedCouponModel?
    fun findByCouponIdAndUserId(couponId: Long, userId: Long): IssuedCouponModel?
    fun findAllByCouponId(couponId: Long, pageable: Pageable): Slice<IssuedCouponModel>
    fun findAllByUserId(userId: Long, pageable: Pageable): Slice<IssuedCouponModel>
    fun save(issuedCoupon: IssuedCouponModel): IssuedCouponModel
}
