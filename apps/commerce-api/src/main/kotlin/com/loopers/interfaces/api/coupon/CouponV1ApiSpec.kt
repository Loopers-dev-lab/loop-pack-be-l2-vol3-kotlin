package com.loopers.interfaces.api.coupon

import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.auth.LoginUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "Coupon V1 API", description = "쿠폰 대고객 API")
interface CouponV1ApiSpec {

    @Operation(summary = "쿠폰 발급", description = "쿠폰을 발급받습니다. 동일 쿠폰은 1인 1회만 발급 가능합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "발급 성공"),
            SwaggerResponse(responseCode = "400", description = "만료된 쿠폰"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 쿠폰"),
            SwaggerResponse(responseCode = "409", description = "이미 발급받은 쿠폰"),
        ],
    )
    fun issueCoupon(loginUser: LoginUser, couponId: Long): ApiResponse<CouponV1Dto.CouponIssueResponse>

    @Operation(summary = "내 쿠폰 목록 조회", description = "내가 발급받은 쿠폰 목록을 조회합니다. AVAILABLE/USED/EXPIRED 상태를 포함합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    fun getMyCoupons(loginUser: LoginUser): ApiResponse<List<CouponV1Dto.MyCouponResponse>>
}
