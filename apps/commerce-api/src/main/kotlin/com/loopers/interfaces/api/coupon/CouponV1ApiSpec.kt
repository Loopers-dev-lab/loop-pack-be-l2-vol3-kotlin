package com.loopers.interfaces.api.coupon

import com.loopers.domain.coupon.dto.CouponInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Coupon V1 API", description = "쿠폰 발급 및 조회 API")
interface CouponV1ApiSpec {

    @Operation(
        summary = "쿠폰 발급",
        description = "쿠폰 템플릿을 선택하여 사용자에게 쿠폰을 발급합니다",
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발급 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 발급받은 쿠폰 또는 유효하지 않은 템플릿"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "쿠폰 템플릿이 존재하지 않음"),
        ],
    )
    fun issueCoupon(
        @Parameter(
            description = "쿠폰 템플릿 ID",
            required = true,
        )
        couponId: Long,
        userId: Long = 0,
    ): ApiResponse<CouponInfo>

    @Operation(
        summary = "내 쿠폰 목록 조회",
        description = "로그인한 사용자의 발급받은 모든 쿠폰 목록을 조회합니다",
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    fun getMyCoupons(
        userId: Long = 0,
        page: Int = 0,
        size: Int = 20,
    ): ApiResponse<PageResponse<CouponInfo>>
}
