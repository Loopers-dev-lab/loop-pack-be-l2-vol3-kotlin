package com.loopers.interfaces.api.coupon

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Coupon Admin V1 API", description = "어드민 쿠폰 API")
interface CouponAdminV1ApiSpec {

    @Operation(summary = "쿠폰 목록 조회", description = "쿠폰 목록을 페이징 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "어드민 인증 실패"),
        ],
    )
    fun getAllCoupons(
        @Parameter(description = "어드민 LDAP 인증", required = true)
        ldap: String?,
        @ParameterObject pageable: Pageable,
    ): ApiResponse<Page<CouponAdminV1Dto.CouponAdminResponse>>

    @Operation(summary = "쿠폰 상세 조회", description = "쿠폰 ID로 쿠폰을 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "어드민 인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "쿠폰 없음"),
        ],
    )
    fun getCoupon(
        @Parameter(description = "어드민 LDAP 인증", required = true)
        ldap: String?,
        @Parameter(description = "쿠폰 ID", required = true)
        couponId: Long,
    ): ApiResponse<CouponAdminV1Dto.CouponAdminResponse>

    @Operation(summary = "쿠폰 등록", description = "새로운 쿠폰을 등록합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "등록 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 요청"),
            SwaggerApiResponse(responseCode = "401", description = "어드민 인증 실패"),
        ],
    )
    fun createCoupon(
        @Parameter(description = "어드민 LDAP 인증", required = true)
        ldap: String?,
        request: CouponAdminV1Dto.CreateRequest,
    ): ApiResponse<CouponAdminV1Dto.CouponAdminResponse>

    @Operation(summary = "쿠폰 수정", description = "쿠폰 정보를 수정합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "수정 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 요청"),
            SwaggerApiResponse(responseCode = "401", description = "어드민 인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "쿠폰 없음"),
        ],
    )
    fun updateCoupon(
        @Parameter(description = "어드민 LDAP 인증", required = true)
        ldap: String?,
        @Parameter(description = "쿠폰 ID", required = true)
        couponId: Long,
        request: CouponAdminV1Dto.UpdateRequest,
    ): ApiResponse<CouponAdminV1Dto.CouponAdminResponse>

    @Operation(summary = "쿠폰 삭제", description = "쿠폰을 삭제합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "삭제 성공"),
            SwaggerApiResponse(responseCode = "401", description = "어드민 인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "쿠폰 없음"),
        ],
    )
    fun deleteCoupon(
        @Parameter(description = "어드민 LDAP 인증", required = true)
        ldap: String?,
        @Parameter(description = "쿠폰 ID", required = true)
        couponId: Long,
    ): ApiResponse<Any>

    @Operation(summary = "쿠폰 발급 내역 조회", description = "쿠폰별 발급 내역을 페이징 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "어드민 인증 실패"),
        ],
    )
    fun getIssuedCoupons(
        @Parameter(description = "어드민 LDAP 인증", required = true)
        ldap: String?,
        @Parameter(description = "쿠폰 ID", required = true)
        couponId: Long,
        @ParameterObject pageable: Pageable,
    ): ApiResponse<Page<CouponAdminV1Dto.IssuedCouponAdminResponse>>
}
