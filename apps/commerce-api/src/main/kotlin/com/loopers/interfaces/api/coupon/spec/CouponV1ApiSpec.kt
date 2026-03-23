package com.loopers.interfaces.api.coupon.spec

import com.loopers.interfaces.api.coupon.dto.CouponV1Dto
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.auth.AuthUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity

@Tag(name = "Coupon V1 API", description = "쿠폰 API")
interface CouponV1ApiSpec {

    @Operation(summary = "쿠폰 발급", description = "쿠폰을 발급받습니다.")
    fun issueCoupon(
        @Parameter(hidden = true) @AuthUser userId: Long,
        @Positive couponId: Long,
    ): ApiResponse<CouponV1Dto.IssueCouponResponse>

    @Operation(summary = "쿠폰 발급 요청 (비동기)", description = "선착순 쿠폰 발급을 요청합니다. 202 Accepted와 requestId를 반환합니다.")
    fun issueAsync(
        @Parameter(hidden = true) @AuthUser userId: Long,
        @Positive couponId: Long,
    ): ResponseEntity<ApiResponse<CouponV1Dto.IssueAsyncResponse>>

    @Operation(summary = "쿠폰 발급 상태 조회", description = "비동기 발급 요청의 처리 상태를 조회합니다.")
    fun getIssueStatus(
        @Parameter(hidden = true) @AuthUser userId: Long,
        @NotBlank requestId: String,
    ): ApiResponse<CouponV1Dto.IssueStatusResponse>

    @Operation(summary = "내 쿠폰 목록 조회", description = "발급받은 쿠폰 목록을 조회합니다.")
    fun getMyCoupons(
        @Parameter(hidden = true) @AuthUser userId: Long,
    ): ApiResponse<List<CouponV1Dto.MyCouponResponse>>
}
