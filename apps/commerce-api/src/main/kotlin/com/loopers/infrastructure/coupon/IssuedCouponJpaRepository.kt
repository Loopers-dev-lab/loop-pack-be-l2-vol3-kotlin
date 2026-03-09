package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.IssuedCoupon
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface IssuedCouponJpaRepository : JpaRepository<IssuedCoupon, Long> {

    fun findAllByUserId(userId: Long): List<IssuedCoupon>

    fun findAllByCouponId(couponId: Long, pageable: Pageable): Page<IssuedCoupon>

    fun existsByCouponIdAndUserId(couponId: Long, userId: Long): Boolean
}
