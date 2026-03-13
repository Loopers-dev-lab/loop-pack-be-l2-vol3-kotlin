package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.config.auth.AuthenticatedMember
import com.loopers.interfaces.config.auth.MemberAuthenticated
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class CouponV1Controller(
    private val couponFacade: CouponFacade,
) : CouponV1ApiSpec {
    @MemberAuthenticated
    @PostMapping("/api/v1/coupons/templates/{templateId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    override fun issueCoupon(
        authenticatedMember: AuthenticatedMember,
        @PathVariable templateId: Long,
    ): ApiResponse<CouponV1Dto.IssuedCouponResponse> {
        return couponFacade.issueCoupon(authenticatedMember.id, templateId)
            .let { CouponV1Dto.IssuedCouponResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @MemberAuthenticated
    @GetMapping("/api/v1/members/me/coupons")
    override fun getMyIssuedCoupons(
        authenticatedMember: AuthenticatedMember,
    ): ApiResponse<List<CouponV1Dto.IssuedCouponResponse>> {
        return couponFacade.getMyIssuedCoupons(authenticatedMember.id)
            .map { CouponV1Dto.IssuedCouponResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
