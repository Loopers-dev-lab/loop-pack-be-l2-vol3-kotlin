package com.loopers.application.coupon

import com.loopers.application.event.CouponIssueRequestOutboxAppender
import com.loopers.domain.coupon.CouponIssueRequestService
import com.loopers.domain.coupon.CouponIssueService
import com.loopers.domain.coupon.CouponService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CouponFacade(
    private val couponService: CouponService,
    private val couponIssueService: CouponIssueService,
    private val couponIssueRequestService: CouponIssueRequestService,
    private val couponIssueRequestOutboxAppender: CouponIssueRequestOutboxAppender,
) {
    @Transactional
    fun issue(couponId: Long, userId: Long): CouponIssueRequestInfo {
        couponService.findById(couponId)
        val issueRequest = couponIssueRequestService.create(couponId, userId)
        couponIssueRequestOutboxAppender.append(issueRequest)
        return CouponIssueRequestInfo.from(issueRequest)
    }

    fun findMyCoupons(userId: Long, pageable: Pageable): Page<CouponIssueInfo> {
        val issues = couponIssueService.findByUserId(userId, pageable)
        val couponIds = issues.content.map { it.couponId }.distinct()
        val couponMap = couponService.findAllByIds(couponIds).associateBy { it.id }

        return issues.map { issue ->
            CouponIssueInfo.of(issue, couponMap[issue.couponId]!!)
        }
    }

    fun findIssueRequest(requestId: Long, userId: Long): CouponIssueRequestInfo {
        val request = couponIssueRequestService.findById(requestId)
        if (request.userId != userId) {
            throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 쿠폰 발급 요청입니다: $requestId")
        }
        return CouponIssueRequestInfo.from(request)
    }
}
