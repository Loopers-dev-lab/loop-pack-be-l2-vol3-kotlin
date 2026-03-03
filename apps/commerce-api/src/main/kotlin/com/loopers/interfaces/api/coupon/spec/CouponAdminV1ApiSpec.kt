package com.loopers.interfaces.api.coupon.spec

import com.loopers.interfaces.api.coupon.dto.CouponAdminV1Dto
import com.loopers.interfaces.support.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.data.domain.Page

@Tag(name = "Coupon Admin V1 API", description = "쿠폰 관리 API")
interface CouponAdminV1ApiSpec {

    @Operation(summary = "쿠폰 목록 조회 (관리자)", description = "관리자가 쿠폰 목록을 조회합니다.")
    fun getCoupons(
        @PositiveOrZero page: Int,
        @Positive @Max(100) size: Int,
    ): ApiResponse<Page<CouponAdminV1Dto.CouponAdminResponse>>

    @Operation(summary = "쿠폰 생성", description = "쿠폰을 생성합니다.")
    fun createCoupon(
        @Valid request: CouponAdminV1Dto.CreateCouponRequest,
    ): ApiResponse<CouponAdminV1Dto.CouponAdminResponse>

    @Operation(summary = "쿠폰 상세 조회 (관리자)", description = "관리자가 쿠폰을 조회합니다.")
    fun getCoupon(couponId: Long): ApiResponse<CouponAdminV1Dto.CouponAdminResponse>

    @Operation(summary = "쿠폰 수정", description = "쿠폰을 수정합니다.")
    fun updateCoupon(
        couponId: Long,
        @Valid request: CouponAdminV1Dto.UpdateCouponRequest,
    ): ApiResponse<CouponAdminV1Dto.CouponAdminResponse>

    @Operation(summary = "쿠폰 삭제", description = "쿠폰을 삭제합니다.")
    fun deleteCoupon(couponId: Long): ApiResponse<Any>

    @Operation(summary = "쿠폰 발급 내역 조회 (관리자)", description = "관리자가 쿠폰 발급 내역을 조회합니다.")
    fun getCouponIssues(
        couponId: Long,
        @PositiveOrZero page: Int,
        @Positive @Max(100) size: Int,
    ): ApiResponse<Page<CouponAdminV1Dto.IssuedCouponAdminResponse>>
}
