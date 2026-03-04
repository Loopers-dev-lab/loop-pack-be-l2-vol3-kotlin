package com.loopers.interfaces.api.coupon

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page

@Tag(name = "Admin Coupon V1 API", description = "관리자 쿠폰 관련 API")
interface AdminCouponV1ApiSpec {

    @Operation(summary = "쿠폰 템플릿 등록", description = "새로운 쿠폰 템플릿을 등록합니다.")
    fun register(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
        request: AdminCouponV1Dto.RegisterRequest,
    ): ApiResponse<AdminCouponV1Dto.DetailResponse>

    @Operation(summary = "쿠폰 템플릿 목록 조회", description = "쿠폰 템플릿 목록을 조회합니다.")
    fun getAll(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
        page: Int,
        size: Int,
    ): ApiResponse<Page<AdminCouponV1Dto.MainResponse>>

    @Operation(summary = "쿠폰 템플릿 상세 조회", description = "쿠폰 템플릿 상세 정보를 조회합니다.")
    fun getById(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
        couponId: Long,
    ): ApiResponse<AdminCouponV1Dto.DetailResponse>

    @Operation(summary = "쿠폰 템플릿 수정", description = "쿠폰 템플릿 정보를 수정합니다.")
    fun update(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
        couponId: Long,
        request: AdminCouponV1Dto.UpdateRequest,
    ): ApiResponse<AdminCouponV1Dto.DetailResponse>

    @Operation(summary = "쿠폰 템플릿 삭제", description = "쿠폰 템플릿을 삭제합니다.")
    fun delete(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
        couponId: Long,
    ): ApiResponse<Any>

    @Operation(summary = "쿠폰 발급 내역 조회", description = "특정 쿠폰의 발급 내역을 조회합니다.")
    fun getIssuedCoupons(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
        couponId: Long,
        page: Int,
        size: Int,
    ): ApiResponse<Page<AdminCouponV1Dto.IssuedMainResponse>>
}
