package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssue
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface CouponIssueJpaRepository : JpaRepository<CouponIssue, Long> {

    fun findByIdAndDeletedAtIsNull(id: Long): CouponIssue?

    fun findByUserIdAndCouponId(userId: Long, couponId: Long): CouponIssue?

    fun findAllByUserId(userId: Long): List<CouponIssue>

    fun findAllByCouponId(couponId: Long, pageable: Pageable): Page<CouponIssue>
}
