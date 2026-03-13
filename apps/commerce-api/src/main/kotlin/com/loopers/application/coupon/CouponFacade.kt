package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponIssueService
import com.loopers.domain.coupon.CouponService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class CouponFacade(
    private val couponService: CouponService,
    private val couponIssueService: CouponIssueService,
) {
    fun issue(couponId: Long, userId: Long): CouponIssueInfo {
        val couponIssue = couponIssueService.issue(couponId, userId)
        val coupon = couponService.findById(couponId)
        return CouponIssueInfo.of(couponIssue, coupon)
    }

    fun findMyCoupons(userId: Long, pageable: Pageable): Page<CouponIssueInfo> {
        val issues = couponIssueService.findByUserId(userId, pageable)
        val couponIds = issues.content.map { it.couponId }.distinct()
        val couponMap = couponService.findAllByIds(couponIds).associateBy { it.id }

        return issues.map { issue ->
            CouponIssueInfo.of(issue, couponMap[issue.couponId]!!)
        }
    }
}
