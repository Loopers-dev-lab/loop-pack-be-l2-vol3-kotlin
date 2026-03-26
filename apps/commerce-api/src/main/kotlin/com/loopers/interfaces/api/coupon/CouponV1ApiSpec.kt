package com.loopers.interfaces.api.coupon

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "Coupon V1 API", description = "쿠폰 관련 사용자 API 입니다.")
interface CouponV1ApiSpec {
    @Operation(
        summary = "쿠폰 발급",
        description = "쿠폰을 발급받습니다.",
    )
    @SwaggerResponse(responseCode = "201", description = "발급 성공")
    fun issueCoupon(
        loginId: String,
        loginPw: String,
        couponId: Long,
    ): ApiResponse<CouponV1Dto.IssuedCouponResponse>

    @Operation(
        summary = "내 쿠폰 목록 조회",
        description = "발급받은 쿠폰 목록을 조회합니다.",
    )
    @SwaggerResponse(responseCode = "200", description = "조회 성공")
    fun getMyCoupons(
        loginId: String,
        loginPw: String,
        page: Int,
        size: Int,
    ): ApiResponse<CouponV1Dto.IssuedCouponsResponse>

    @Operation(
        summary = "선착순 쿠폰 발급 요청",
        description = "비동기로 쿠폰 발급을 요청합니다.",
    )
    @SwaggerResponse(responseCode = "202", description = "요청 접수 완료")
    fun issueAsyncCoupon(
        loginId: String,
        loginPw: String,
        couponId: Long,
    ): ApiResponse<CouponV1Dto.CouponIssueRequestResponse>

    @Operation(
        summary = "쿠폰 발급 요청 상태 조회",
        description = "비동기 쿠폰 발급 요청의 처리 상태를 조회합니다.",
    )
    @SwaggerResponse(responseCode = "200", description = "조회 성공")
    fun getCouponIssueStatus(
        requestId: Long,
    ): ApiResponse<CouponV1Dto.CouponIssueStatusResponse>
}
