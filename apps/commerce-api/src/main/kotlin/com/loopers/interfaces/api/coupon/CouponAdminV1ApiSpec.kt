package com.loopers.interfaces.api.coupon

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "Coupon Admin V1 API", description = "쿠폰 관리자 API")
interface CouponAdminV1ApiSpec {

    @Operation(summary = "쿠폰 템플릿 목록 조회", description = "전체 쿠폰 템플릿 목록을 페이징하여 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    fun getCoupons(page: Int, size: Int): ApiResponse<Page<CouponAdminV1Dto.CouponAdminResponse>>

    @Operation(summary = "쿠폰 템플릿 상세 조회", description = "쿠폰 템플릿 상세 정보를 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 쿠폰"),
        ],
    )
    fun getCoupon(couponId: Long): ApiResponse<CouponAdminV1Dto.CouponAdminResponse>

    @Operation(summary = "쿠폰 템플릿 등록", description = "새로운 쿠폰 템플릿을 등록합니다. FIXED(정액) / RATE(정률) 타입을 지정합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "등록 성공"),
            SwaggerResponse(responseCode = "400", description = "잘못된 요청"),
        ],
    )
    fun createCoupon(request: CouponAdminV1Dto.CreateCouponRequest): ApiResponse<CouponAdminV1Dto.CouponAdminResponse>

    @Operation(summary = "쿠폰 템플릿 수정", description = "쿠폰 템플릿 정보를 수정합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "수정 성공"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 쿠폰"),
        ],
    )
    fun updateCoupon(
        couponId: Long,
        request: CouponAdminV1Dto.UpdateCouponRequest,
    ): ApiResponse<CouponAdminV1Dto.CouponAdminResponse>

    @Operation(summary = "쿠폰 템플릿 삭제", description = "쿠폰 템플릿을 삭제합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "삭제 성공"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 쿠폰"),
        ],
    )
    fun deleteCoupon(couponId: Long): ApiResponse<Unit>

    @Operation(summary = "쿠폰 발급 내역 조회", description = "특정 쿠폰의 발급 내역을 페이징하여 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    fun getCouponIssues(couponId: Long, page: Int, size: Int): ApiResponse<Page<CouponAdminV1Dto.CouponIssueAdminResponse>>
}
