package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssueRequestModel
import org.springframework.data.jpa.repository.JpaRepository

interface CouponIssueRequestJpaRepository : JpaRepository<CouponIssueRequestModel, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): CouponIssueRequestModel?
    fun findByCouponIdAndUserIdAndDeletedAtIsNull(couponId: Long, userId: Long): CouponIssueRequestModel?
}
