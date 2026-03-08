package com.loopers.interfaces.api.coupon

import com.loopers.interfaces.common.ApiResponse
import com.loopers.support.auth.AuthenticatedUserInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Coupon API", description = "쿠폰 API")
interface CouponApiSpec {

    @Operation(
        summary = "쿠폰 발급",
        description = "사용자에게 쿠폰을 발급합니다.",
    )
    fun issue(userInfo: AuthenticatedUserInfo, couponId: Long): ApiResponse<Unit>
}
