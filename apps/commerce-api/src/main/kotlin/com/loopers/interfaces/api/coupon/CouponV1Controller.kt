package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponFacade
import com.loopers.domain.user.User
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.security.LoginUser
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/coupons")
class CouponV1Controller(
    private val couponFacade: CouponFacade,
) : CouponV1ApiSpec {

    @PostMapping("/issue")
    override fun issueCoupon(
        @LoginUser user: User,
        @RequestBody request: CouponV1Dto.IssueCouponRequest,
    ): ApiResponse<CouponV1Dto.UserCouponResponse> =
        couponFacade.issueCoupon(user.id, request.couponTemplateId)
            .let { CouponV1Dto.UserCouponResponse.from(it) }
            .let { ApiResponse.success(it) }

    @GetMapping("/me")
    override fun getUserCoupons(
        @LoginUser user: User,
    ): ApiResponse<List<CouponV1Dto.UserCouponResponse>> =
        couponFacade.getUserCoupons(user.id)
            .map { CouponV1Dto.UserCouponResponse.from(it) }
            .let { ApiResponse.success(it) }
}
