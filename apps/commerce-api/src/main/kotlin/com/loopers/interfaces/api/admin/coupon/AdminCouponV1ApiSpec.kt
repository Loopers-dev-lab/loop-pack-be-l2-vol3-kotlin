package com.loopers.interfaces.api.admin.coupon

import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Admin - 쿠폰 관리")
interface AdminCouponV1ApiSpec {
    @Operation(summary = "쿠폰 템플릿 등록", description = "새로운 쿠폰 템플릿을 등록합니다.")
    fun register(
        ldap: String,
        request: AdminCouponV1Request.Register,
    ): ApiResponse<AdminCouponV1Response.Register>

    @Operation(summary = "쿠폰 템플릿 목록 조회", description = "쿠폰 템플릿 목록을 페이징 조회합니다.")
    fun getList(
        ldap: String,
        pageRequest: PageRequest,
    ): ApiResponse<PageResponse<AdminCouponV1Response.Summary>>

    @Operation(summary = "쿠폰 템플릿 상세 조회", description = "쿠폰 템플릿 상세 정보를 조회합니다.")
    fun getDetail(
        ldap: String,
        couponId: Long,
    ): ApiResponse<AdminCouponV1Response.Detail>

    @Operation(summary = "쿠폰 템플릿 수정", description = "쿠폰 템플릿 정보를 수정합니다.")
    fun update(
        ldap: String,
        couponId: Long,
        request: AdminCouponV1Request.Update,
    ): ApiResponse<AdminCouponV1Response.Update>

    @Operation(summary = "쿠폰 템플릿 삭제", description = "쿠폰 템플릿을 삭제합니다.")
    fun delete(
        ldap: String,
        couponId: Long,
    ): ApiResponse<Any>

    @Operation(summary = "쿠폰 발급 내역 조회", description = "특정 쿠폰의 발급 내역을 페이징 조회합니다.")
    fun getIssueList(
        ldap: String,
        couponId: Long,
        pageRequest: PageRequest,
    ): ApiResponse<PageResponse<AdminCouponV1Response.IssuedCouponItem>>
}
