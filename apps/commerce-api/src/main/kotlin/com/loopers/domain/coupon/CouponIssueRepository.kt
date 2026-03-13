package com.loopers.domain.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CouponIssueRepository {
    fun save(couponIssue: CouponIssueModel): CouponIssueModel
    fun findByIdAndDeletedAtIsNull(id: Long): CouponIssueModel?
    fun findByCouponIdAndUserIdAndDeletedAtIsNull(couponId: Long, userId: Long): CouponIssueModel?
    fun findAllByUserIdAndDeletedAtIsNull(userId: Long, pageable: Pageable): Page<CouponIssueModel>
    fun findAllByCouponIdAndDeletedAtIsNull(couponId: Long, pageable: Pageable): Page<CouponIssueModel>
    fun findByIdForUpdate(id: Long): CouponIssueModel?
}
