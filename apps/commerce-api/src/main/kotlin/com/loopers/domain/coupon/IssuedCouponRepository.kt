package com.loopers.domain.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface IssuedCouponRepository {
    fun save(issuedCoupon: IssuedCoupon): IssuedCoupon
    fun findByIdAndDeletedAtIsNull(id: Long): IssuedCoupon?
    fun findAllByUserIdAndDeletedAtIsNull(userId: Long): List<IssuedCoupon>
    fun findAllByCouponTemplateIdAndDeletedAtIsNull(couponTemplateId: Long, pageable: Pageable): Page<IssuedCoupon>
}
