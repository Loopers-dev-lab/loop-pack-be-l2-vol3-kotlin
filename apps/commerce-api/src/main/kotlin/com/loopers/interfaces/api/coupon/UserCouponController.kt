package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponFacade
import com.loopers.interfaces.common.ApiResponse
import com.loopers.support.auth.AuthenticatedUser
import com.loopers.support.auth.AuthenticatedUserInfo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me/coupons")
class UserCouponController(
    private val couponFacade: CouponFacade,
) : UserCouponApiSpec {

    @GetMapping
    override fun getMyCoupons(
        @AuthenticatedUser userInfo: AuthenticatedUserInfo,
    ): ApiResponse<List<CouponDto.MyCouponResponse>> {
        val coupons = couponFacade.getMyCoupons(userInfo.id)
        return ApiResponse.success(coupons.map { CouponDto.MyCouponResponse.from(it) })
    }
}
