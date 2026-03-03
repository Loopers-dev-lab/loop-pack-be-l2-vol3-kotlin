package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.GetMyCouponsUseCase
import com.loopers.application.coupon.IssueCouponUseCase
import com.loopers.application.coupon.IssuedCouponInfo
import com.loopers.interfaces.api.coupon.dto.CouponV1Dto
import com.loopers.interfaces.api.coupon.spec.CouponV1ApiSpec
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.auth.AuthUser
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class CouponV1Controller(
    private val issueCouponUseCase: IssueCouponUseCase,
    private val getMyCouponsUseCase: GetMyCouponsUseCase,
) : CouponV1ApiSpec {

    @PostMapping("/api/v1/coupons/{couponId}/issue")
    override fun issueCoupon(
        @AuthUser userId: Long,
        @PathVariable couponId: Long,
    ): ApiResponse<IssuedCouponInfo> {
        return issueCouponUseCase.execute(userId, couponId)
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/api/v1/users/me/coupons")
    override fun getMyCoupons(
        @AuthUser userId: Long,
    ): ApiResponse<List<CouponV1Dto.MyCouponResponse>> {
        return getMyCouponsUseCase.execute(userId)
            .map { CouponV1Dto.MyCouponResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
