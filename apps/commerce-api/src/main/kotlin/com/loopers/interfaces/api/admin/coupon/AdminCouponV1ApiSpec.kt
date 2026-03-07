package com.loopers.interfaces.api.admin.coupon

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Admin Coupon V1 API", description = "어드민 쿠폰 API 입니다.")
interface AdminCouponV1ApiSpec {
    @Operation(summary = "쿠폰 템플릿 등록", description = "새로운 쿠폰 템플릿을 등록합니다.")
    fun createTemplate(request: AdminCouponV1Dto.CreateRequest): ApiResponse<AdminCouponV1Dto.TemplateResponse>

    @Operation(summary = "쿠폰 템플릿 목록 조회", description = "쿠폰 템플릿 목록을 조회합니다.")
    fun getTemplates(page: Int, size: Int): ApiResponse<PageResponse<AdminCouponV1Dto.TemplateResponse>>

    @Operation(summary = "쿠폰 템플릿 상세 조회", description = "쿠폰 템플릿 상세 정보를 조회합니다.")
    fun getTemplate(couponId: Long): ApiResponse<AdminCouponV1Dto.TemplateResponse>

    @Operation(summary = "쿠폰 템플릿 수정", description = "쿠폰 템플릿 정보를 수정합니다.")
    fun updateTemplate(couponId: Long, request: AdminCouponV1Dto.UpdateRequest): ApiResponse<AdminCouponV1Dto.TemplateResponse>

    @Operation(summary = "쿠폰 템플릿 삭제", description = "쿠폰 템플릿을 삭제합니다.")
    fun deleteTemplate(couponId: Long): ApiResponse<Any>

    @Operation(summary = "발급 쿠폰 목록 조회", description = "특정 쿠폰 템플릿의 발급 쿠폰 목록을 조회합니다.")
    fun getIssuedCouponsByTemplate(
        couponId: Long,
        page: Int,
        size: Int,
    ): ApiResponse<PageResponse<AdminCouponV1Dto.IssuedCouponResponse>>
}
