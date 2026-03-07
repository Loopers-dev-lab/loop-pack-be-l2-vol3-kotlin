package com.loopers.interfaces.api.coupon

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.config.auth.AuthenticatedMember
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Coupon V1 API", description = "쿠폰 API 입니다.")
interface CouponV1ApiSpec {
    @Operation(summary = "쿠폰 발급", description = "쿠폰 템플릿으로부터 쿠폰을 발급받습니다.")
    fun issueCoupon(
        authenticatedMember: AuthenticatedMember,
        templateId: Long,
    ): ApiResponse<CouponV1Dto.IssuedCouponResponse>

    @Operation(summary = "내 쿠폰 목록 조회", description = "발급받은 쿠폰 목록을 조회합니다.")
    fun getMyIssuedCoupons(
        authenticatedMember: AuthenticatedMember,
    ): ApiResponse<List<CouponV1Dto.IssuedCouponResponse>>
}
