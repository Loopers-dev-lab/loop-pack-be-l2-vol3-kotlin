package com.loopers.interfaces.api.coupon

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "Coupon V1 Admin API", description = "쿠폰 관련 어드민 API 입니다.")
interface CouponV1AdminApiSpec {
    @Operation(
        summary = "쿠폰 템플릿 목록 조회",
        description = "쿠폰 템플릿 목록을 조회합니다.",
    )
    @SwaggerResponse(responseCode = "200", description = "조회 성공")
    fun getCouponTemplates(
        ldap: String,
        page: Int,
        size: Int,
    ): ApiResponse<CouponV1AdminDto.CouponTemplatesResponse>

    @Operation(
        summary = "쿠폰 템플릿 상세 조회",
        description = "특정 쿠폰 템플릿 상세 내용을 조회합니다.",
    )
    @SwaggerResponse(responseCode = "200", description = "조회 성공")
    fun getCouponTemplateDetail(
        ldap: String,
        couponId: Long,
    ): ApiResponse<CouponV1AdminDto.CouponTemplateDetailResponse>

    @Operation(
        summary = "쿠폰 템플릿 등록",
        description = "정액 또는 정률 타입의 쿠폰 템플릿을 등록합니다.",
    )
    @SwaggerResponse(responseCode = "201", description = "등록 성공")
    fun register(
        ldap: String,
        request: CouponV1AdminDto.RegisterRequest,
    )

    @Operation(
        summary = "쿠폰 템플릿 수정",
        description = "쿠폰 템플릿을 수정합니다.",
    )
    @SwaggerResponse(responseCode = "204", description = "수정 성공")
    fun modify(
        ldap: String,
        couponId: Long,
        request: CouponV1AdminDto.ModifyRequest,
    )

    @Operation(
        summary = "쿠폰 템플릿 삭제",
        description = "쿠폰 템플릿을 삭제합니다.",
    )
    @SwaggerResponse(responseCode = "204", description = "삭제 성공")
    fun delete(
        ldap: String,
        couponId: Long,
    )

    @Operation(
        summary = "쿠폰 발급 내역 조회",
        description = "쿠폰 발급 내역을 조회합니다.",
    )
    @SwaggerResponse(responseCode = "200", description = "조회 성공")
    fun getIssuedCoupons(
        ldap: String,
        couponId: Long,
        page: Int,
        size: Int,
    ): ApiResponse<CouponV1AdminDto.IssuedCouponsResponse>
}
