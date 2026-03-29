package com.loopers.interfaces.api.coupon

import com.loopers.interfaces.common.ApiResponse
import com.loopers.support.auth.AuthenticatedUserInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Coupon Issue Async API", description = "선착순 쿠폰 발급 API")
interface CouponIssueAsyncApiSpec {

    @Operation(
        summary = "선착순 쿠폰 발급 요청",
        description = "선착순 쿠폰 발급을 비동기로 요청합니다. 202 Accepted와 함께 requestId를 반환합니다.",
    )
    fun issueAsync(userInfo: AuthenticatedUserInfo, couponId: Long): ApiResponse<CouponIssueAsyncDto.IssueAsyncResponse>

    @Operation(
        summary = "쿠폰 발급 요청 결과 조회",
        description = "requestId로 쿠폰 발급 요청의 현재 상태를 조회합니다.",
    )
    fun getIssueRequest(userInfo: AuthenticatedUserInfo, requestId: String): ApiResponse<CouponIssueAsyncDto.IssueRequestResponse>
}
