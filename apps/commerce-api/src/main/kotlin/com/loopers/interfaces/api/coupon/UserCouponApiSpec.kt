package com.loopers.interfaces.api.coupon

import com.loopers.interfaces.common.ApiResponse
import com.loopers.support.auth.AuthenticatedUserInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "User Coupon API", description = "사용자 쿠폰 API")
interface UserCouponApiSpec {

    @Operation(
        summary = "내 쿠폰 목록 조회",
        description = "사용자가 발급받은 쿠폰 목록을 상태와 함께 조회합니다.",
    )
    fun getMyCoupons(userInfo: AuthenticatedUserInfo): ApiResponse<List<CouponDto.MyCouponResponse>>
}
