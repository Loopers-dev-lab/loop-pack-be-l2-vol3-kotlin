package com.loopers.interfaces.api.coupon

import com.loopers.application.auth.AuthUseCase
import com.loopers.application.coupon.CouponUseCase
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class CouponV1Controller(
    private val authUseCase: AuthUseCase,
    private val couponUseCase: CouponUseCase,
) : CouponV1ApiSpec {

    @PostMapping("/api/v1/coupons/{couponId}/issue")
    override fun issueCoupon(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable couponId: Long,
    ): ApiResponse<CouponV1Dto.IssuedDetailResponse> {
        val member = authUseCase.authenticate(loginId, password)

        return couponUseCase.issueCoupon(couponId, member.id!!)
            .let { CouponV1Dto.IssuedDetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/api/v1/users/me/coupons")
    override fun getMyCoupons(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ApiResponse<List<CouponV1Dto.IssuedDetailResponse>> {
        val member = authUseCase.authenticate(loginId, password)

        return couponUseCase.getMyCoupons(member.id!!)
            .map { CouponV1Dto.IssuedDetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
