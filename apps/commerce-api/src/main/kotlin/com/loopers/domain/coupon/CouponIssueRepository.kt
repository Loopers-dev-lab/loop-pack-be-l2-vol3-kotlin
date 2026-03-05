package com.loopers.domain.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CouponIssueRepository {
    fun save(couponIssue: CouponIssue): CouponIssue
    fun findById(id: Long): CouponIssue?
    fun findByUserIdAndCouponId(userId: Long, couponId: Long): CouponIssue?
    fun findAllByUserId(userId: Long): List<CouponIssue>
    fun findAllByCouponId(couponId: Long, pageable: Pageable): Page<CouponIssue>
}
