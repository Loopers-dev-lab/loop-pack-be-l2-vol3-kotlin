package com.loopers.interfaces.api.coupon

import com.loopers.domain.user.User
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.security.AuthHeader
import com.loopers.interfaces.api.security.LoginUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Coupon V1 API", description = "쿠폰 사용자 API입니다.")
interface CouponV1ApiSpec {

    @Operation(
        summary = "쿠폰 발급",
        description = "사용자에게 쿠폰을 발급합니다.",
        parameters = [
            Parameter(name = AuthHeader.HEADER_LOGIN_ID, `in` = ParameterIn.HEADER, required = true),
            Parameter(name = AuthHeader.HEADER_LOGIN_PW, `in` = ParameterIn.HEADER, required = true, hidden = true),
        ]
    )
    fun issueCoupon(@LoginUser user: User, request: CouponV1Dto.IssueCouponRequest): ApiResponse<CouponV1Dto.UserCouponResponse>

    @Operation(
        summary = "보유 쿠폰 목록 조회",
        description = "사용자가 보유한 쿠폰 목록을 조회합니다.",
        parameters = [
            Parameter(name = AuthHeader.HEADER_LOGIN_ID, `in` = ParameterIn.HEADER, required = true),
            Parameter(name = AuthHeader.HEADER_LOGIN_PW, `in` = ParameterIn.HEADER, required = true, hidden = true),
        ]
    )
    fun getUserCoupons(@LoginUser user: User): ApiResponse<List<CouponV1Dto.UserCouponResponse>>
}
