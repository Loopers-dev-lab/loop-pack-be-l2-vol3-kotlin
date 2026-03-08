package com.loopers.interfaces.admin.coupon

import com.loopers.domain.coupon.dto.CouponTemplateDetailInfo
import com.loopers.domain.coupon.dto.IssuedCouponInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Admin Coupon V1 API", description = "관리자 쿠폰 관리 API")
interface AdminCouponV1ApiSpec {

    @Operation(
        summary = "쿠폰 템플릿 목록 조회",
        description = "쿠폰 템플릿 목록을 페이징하여 조회합니다",
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
            ),
        ],
    )
    fun getCouponTemplates(
        page: Int = 0,
        size: Int = 20,
    ): ApiResponse<PageResponse<CouponTemplateDetailInfo>>

    @Operation(
        summary = "쿠폰 템플릿 상세 조회",
        description = "특정 쿠폰 템플릿의 상세 정보를 조회합니다",
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "템플릿 없음",
            ),
        ],
    )
    fun getCouponTemplate(
        @Parameter(description = "쿠폰 템플릿 ID", required = true)
        templateId: Long,
    ): ApiResponse<CouponTemplateDetailInfo>

    @Operation(
        summary = "쿠폰 템플릿 생성",
        description = "새로운 쿠폰 템플릿을 생성합니다",
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "생성 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "입력값 검증 실패",
            ),
        ],
    )
    fun createCouponTemplate(
        request: AdminCouponV1Dto.CreateCouponTemplateRequest,
    ): ApiResponse<CouponTemplateDetailInfo>

    @Operation(
        summary = "쿠폰 템플릿 수정",
        description = "쿠폰 템플릿의 정보를 수정합니다",
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "수정 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "템플릿 없음",
            ),
        ],
    )
    fun updateCouponTemplate(
        @Parameter(description = "쿠폰 템플릿 ID", required = true)
        templateId: Long,
        request: AdminCouponV1Dto.UpdateCouponTemplateRequest,
    ): ApiResponse<CouponTemplateDetailInfo>

    @Operation(
        summary = "쿠폰 템플릿 삭제",
        description = "쿠폰 템플릿을 삭제합니다",
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "삭제 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "템플릿 없음",
            ),
        ],
    )
    fun deleteCouponTemplate(
        @Parameter(description = "쿠폰 템플릿 ID", required = true)
        templateId: Long,
    ): ApiResponse<Any>

    @Operation(
        summary = "발급된 쿠폰 목록 조회",
        description = "특정 쿠폰 템플릿에서 발급된 쿠폰 목록을 조회합니다",
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "템플릿 없음",
            ),
        ],
    )
    fun getIssuedCoupons(
        @Parameter(description = "쿠폰 템플릿 ID", required = true)
        templateId: Long,
        page: Int = 0,
        size: Int = 20,
    ): ApiResponse<PageResponse<IssuedCouponInfo>>
}
