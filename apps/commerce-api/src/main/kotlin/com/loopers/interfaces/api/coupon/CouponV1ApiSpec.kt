package com.loopers.interfaces.api.coupon

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Coupon V1 API", description = "쿠폰 API")
interface CouponV1ApiSpec {
    @Operation(summary = "쿠폰 발급", description = "쿠폰 템플릿 ID로 쿠폰을 발급합니다.")
    fun issueCoupon(loginId: String, password: String, couponId: Long): ApiResponse<CouponV1Dto.IssuedCouponResponse>

    @Operation(summary = "내 쿠폰 목록 조회", description = "내 쿠폰 목록을 조회합니다.")
    fun getMyCoupons(loginId: String, password: String): ApiResponse<List<CouponV1Dto.IssuedCouponResponse>>
}
