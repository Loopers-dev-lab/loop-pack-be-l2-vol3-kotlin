package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.IssuedCoupon
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface IssuedCouponJpaRepository : JpaRepository<IssuedCoupon, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): IssuedCoupon?
    fun findAllByUserIdAndDeletedAtIsNull(userId: Long): List<IssuedCoupon>
    fun findAllByCouponTemplateIdAndDeletedAtIsNull(couponTemplateId: Long, pageable: Pageable): Page<IssuedCoupon>
}
