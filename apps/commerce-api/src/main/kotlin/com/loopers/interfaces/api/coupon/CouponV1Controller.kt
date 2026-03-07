package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponService
import com.loopers.application.user.UserService
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class CouponV1Controller(
    private val couponService: CouponService,
    private val userService: UserService,
) : CouponV1ApiSpec {

    @PostMapping("/api/v1/coupons/{couponId}/issue")
    override fun issueCoupon(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable couponId: Long,
    ): ApiResponse<CouponV1Dto.IssuedCouponResponse> {
        val authUser = userService.authenticate(loginId, password)
        return couponService.issueCoupon(couponId, authUser.id)
            .let { CouponV1Dto.IssuedCouponResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/api/v1/users/me/coupons")
    override fun getMyCoupons(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ApiResponse<List<CouponV1Dto.IssuedCouponResponse>> {
        val authUser = userService.authenticate(loginId, password)
        return couponService.getMyCoupons(authUser.id)
            .map { CouponV1Dto.IssuedCouponResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
