package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.auth.CurrentUser
import com.loopers.support.auth.LoginUser
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CouponV1Controller(
    private val couponFacade: CouponFacade,
) : CouponV1ApiSpec {

    @PostMapping("/api/v1/coupons/{couponId}/issue")
    override fun issueCoupon(
        @CurrentUser loginUser: LoginUser,
        @PathVariable couponId: Long,
    ): ApiResponse<CouponV1Dto.CouponIssueResponse> {
        return couponFacade.issueCoupon(couponId, loginUser.id)
            .let { CouponV1Dto.CouponIssueResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/api/v1/users/me/coupons")
    override fun getMyCoupons(
        @CurrentUser loginUser: LoginUser,
    ): ApiResponse<List<CouponV1Dto.MyCouponResponse>> {
        return couponFacade.getMyCoupons(loginUser.id)
            .map { CouponV1Dto.MyCouponResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
