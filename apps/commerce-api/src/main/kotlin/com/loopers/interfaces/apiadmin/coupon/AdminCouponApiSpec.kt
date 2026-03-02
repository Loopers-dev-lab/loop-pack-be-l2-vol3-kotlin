package com.loopers.interfaces.apiadmin.coupon

import com.loopers.interfaces.common.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Admin Coupon API", description = "어드민 쿠폰 API")
interface AdminCouponApiSpec {
    @Operation(
        summary = "쿠폰 템플릿 목록 조회",
        description = "쿠폰 템플릿 목록을 페이징하여 조회합니다.",
    )
    fun getCoupons(page: Int, size: Int): ApiResponse<AdminCouponDto.PageResponse>

    @Operation(
        summary = "쿠폰 템플릿 상세 조회",
        description = "쿠폰 ID로 상세 정보를 조회합니다.",
    )
    fun getCoupon(couponId: Long): ApiResponse<AdminCouponDto.DetailResponse>
}
