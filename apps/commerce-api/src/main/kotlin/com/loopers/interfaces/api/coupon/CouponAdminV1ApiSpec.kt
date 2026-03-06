package com.loopers.interfaces.api.coupon

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Coupon Admin V1 API", description = "쿠폰 관리 Admin API")
interface CouponAdminV1ApiSpec {

    @Operation(
        summary = "쿠폰 템플릿 목록 조회",
        description = "쿠폰 템플릿 목록을 페이지 단위로 조회합니다.",
        parameters = [
            Parameter(
                name = "X-Loopers-Ldap",
                description = "LDAP 인증 헤더 (값: loopers.admin)",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
        ],
    )
    fun getTemplates(pageable: Pageable): ApiResponse<Page<CouponAdminV1Dto.CouponTemplateResponse>>

    @Operation(
        summary = "쿠폰 템플릿 상세 조회",
        description = "쿠폰 템플릿 ID로 상세 정보를 조회합니다.",
        parameters = [
            Parameter(
                name = "X-Loopers-Ldap",
                description = "LDAP 인증 헤더 (값: loopers.admin)",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
        ],
    )
    fun getTemplateById(@PathVariable couponId: Long): ApiResponse<CouponAdminV1Dto.CouponTemplateResponse>

    @Operation(
        summary = "쿠폰 템플릿 등록",
        description = "새로운 쿠폰 템플릿을 등록합니다.",
        parameters = [
            Parameter(
                name = "X-Loopers-Ldap",
                description = "LDAP 인증 헤더 (값: loopers.admin)",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
        ],
    )
    fun createTemplate(@RequestBody request: CouponAdminV1Dto.CreateCouponTemplateRequest): ApiResponse<CouponAdminV1Dto.CouponTemplateResponse>

    @Operation(
        summary = "쿠폰 템플릿 수정",
        description = "쿠폰 템플릿 ID로 쿠폰 정보를 수정합니다.",
        parameters = [
            Parameter(
                name = "X-Loopers-Ldap",
                description = "LDAP 인증 헤더 (값: loopers.admin)",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
        ],
    )
    fun updateTemplate(
        @PathVariable couponId: Long,
        @RequestBody request: CouponAdminV1Dto.UpdateCouponTemplateRequest,
    ): ApiResponse<CouponAdminV1Dto.CouponTemplateResponse>

    @Operation(
        summary = "쿠폰 템플릿 삭제",
        description = "쿠폰 템플릿 ID로 쿠폰을 삭제합니다.",
        parameters = [
            Parameter(
                name = "X-Loopers-Ldap",
                description = "LDAP 인증 헤더 (값: loopers.admin)",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
        ],
    )
    fun deleteTemplate(@PathVariable couponId: Long): ApiResponse<Unit>

    @Operation(
        summary = "쿠폰 발급 내역 조회",
        description = "특정 쿠폰 템플릿의 발급 내역을 페이지 단위로 조회합니다.",
        parameters = [
            Parameter(
                name = "X-Loopers-Ldap",
                description = "LDAP 인증 헤더 (값: loopers.admin)",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
        ],
    )
    fun getIssues(
        @PathVariable couponId: Long,
        pageable: Pageable,
    ): ApiResponse<Page<CouponAdminV1Dto.UserCouponIssueResponse>>
}
