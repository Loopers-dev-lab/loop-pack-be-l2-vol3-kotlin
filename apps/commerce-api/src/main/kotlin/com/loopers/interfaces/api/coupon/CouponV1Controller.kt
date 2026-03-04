package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthenticatedUser
import com.loopers.interfaces.api.auth.RequireAuth
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/coupons")
class CouponV1Controller(
    private val couponFacade: CouponFacade,
) : CouponV1ApiSpec {

    @PostMapping("/{couponId}/issue")
    override fun issueCoupon(
        @RequireAuth authenticatedUser: AuthenticatedUser,
        @PathVariable couponId: Long,
    ): ApiResponse<CouponV1Dto.UserCouponResponse> =
        couponFacade.issueCoupon(authenticatedUser.loginId, couponId)
            .let { CouponV1Dto.UserCouponResponse.from(it) }
            .let { ApiResponse.success(it) }
}
