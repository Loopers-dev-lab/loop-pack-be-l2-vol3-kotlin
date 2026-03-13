package com.loopers.interfaces.api.user.coupon

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "User Coupon", description = "사용자 쿠폰 API")
interface UserCouponV1ApiSpec {
    @Operation(summary = "쿠폰 발급", description = "쿠폰 템플릿에서 쿠폰을 발급한다")
    fun issue(
        loginId: String,
        password: String,
        couponId: Long,
    ): ApiResponse<UserCouponV1Response.Issued>

    @Operation(summary = "내 쿠폰 목록 조회", description = "발급받은 쿠폰 목록을 조회한다")
    fun getList(
        loginId: String,
        password: String,
    ): ApiResponse<List<UserCouponV1Response.ListItem>>
}
