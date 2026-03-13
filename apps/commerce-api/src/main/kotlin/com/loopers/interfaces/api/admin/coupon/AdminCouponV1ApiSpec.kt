package com.loopers.interfaces.api.admin.coupon

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Admin Coupon V1 API", description = "관리자 쿠폰 API")
interface AdminCouponV1ApiSpec {
    @Operation(summary = "쿠폰 템플릿 목록 조회")
    fun getCouponTemplates(
        page: Int,
        size: Int,
    ): ApiResponse<PageResponse<AdminCouponV1Dto.CouponTemplateResponse>>

    @Operation(summary = "쿠폰 템플릿 상세 조회")
    fun getCouponTemplate(couponId: Long): ApiResponse<AdminCouponV1Dto.CouponTemplateResponse>

    @Operation(summary = "쿠폰 템플릿 등록")
    fun createCouponTemplate(
        req: AdminCouponV1Dto.CreateCouponTemplateRequest,
    ): ApiResponse<AdminCouponV1Dto.CouponTemplateResponse>

    @Operation(summary = "쿠폰 템플릿 수정")
    fun updateCouponTemplate(
        couponId: Long,
        req: AdminCouponV1Dto.UpdateCouponTemplateRequest,
    ): ApiResponse<AdminCouponV1Dto.CouponTemplateResponse>

    @Operation(summary = "쿠폰 템플릿 삭제")
    fun deleteCouponTemplate(couponId: Long): ApiResponse<Any>

    @Operation(summary = "쿠폰 발급 내역 조회")
    fun getIssuedCoupons(
        couponId: Long,
        page: Int,
        size: Int,
    ): ApiResponse<PageResponse<AdminCouponV1Dto.IssuedCouponResponse>>
}
