package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponFacade
import com.loopers.application.coupon.CouponIssueInfo
import com.loopers.domain.auth.AuthenticatedMember
import com.loopers.infrastructure.auth.JwtAuthenticationFilter
import com.loopers.interfaces.api.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CouponV1Controller(
    private val couponFacade: CouponFacade,
) {
    @PostMapping("/api/v1/coupons/{couponId}/issue")
    fun issue(
        @PathVariable couponId: Long,
        request: HttpServletRequest,
    ): ApiResponse<CouponIssueInfo> {
        val member = request.getAttribute(
            JwtAuthenticationFilter.AUTHENTICATED_MEMBER_ATTRIBUTE,
        ) as AuthenticatedMember

        return ApiResponse.success(couponFacade.issue(couponId, member.memberId))
    }

    @GetMapping("/api/v1/users/me/coupons")
    fun findMyCoupons(
        request: HttpServletRequest,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<Page<CouponIssueInfo>> {
        val member = request.getAttribute(
            JwtAuthenticationFilter.AUTHENTICATED_MEMBER_ATTRIBUTE,
        ) as AuthenticatedMember

        return ApiResponse.success(couponFacade.findMyCoupons(member.memberId, pageable))
    }
}
