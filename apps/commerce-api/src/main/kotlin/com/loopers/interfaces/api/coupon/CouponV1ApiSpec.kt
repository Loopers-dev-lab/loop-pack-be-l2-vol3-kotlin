package com.loopers.interfaces.api.coupon

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Coupon V1 API", description = "사용자 쿠폰 관련 API")
interface CouponV1ApiSpec {

    @Operation(summary = "쿠폰 발급", description = "쿠폰을 발급받습니다.")
    fun issueCoupon(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
        couponId: Long,
    ): ApiResponse<CouponV1Dto.IssuedDetailResponse>

    @Operation(summary = "내 쿠폰 목록 조회", description = "내가 발급받은 쿠폰 목록을 조회합니다.")
    fun getMyCoupons(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
    ): ApiResponse<List<CouponV1Dto.IssuedDetailResponse>>
}
