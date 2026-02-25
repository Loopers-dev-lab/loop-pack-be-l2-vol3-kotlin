package com.loopers.interfaces.api.brand

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Brand Admin V1 API", description = "어드민 브랜드 API")
interface BrandAdminV1ApiSpec {

    @Operation(summary = "브랜드 목록 조회", description = "브랜드 목록을 페이징 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "어드민 인증 실패"),
        ],
    )
    fun getAllBrands(
        @Parameter(description = "어드민 LDAP 인증", required = true)
        ldap: String?,
        pageable: Pageable,
    ): ApiResponse<Page<BrandV1Dto.BrandAdminResponse>>

    @Operation(summary = "브랜드 상세 조회", description = "브랜드 ID로 브랜드를 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "어드민 인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "브랜드 없음"),
        ],
    )
    fun getBrand(
        @Parameter(description = "어드민 LDAP 인증", required = true)
        ldap: String?,
        @Parameter(description = "브랜드 ID", required = true)
        brandId: Long,
    ): ApiResponse<BrandV1Dto.BrandAdminResponse>

    @Operation(summary = "브랜드 등록", description = "새로운 브랜드를 등록합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "등록 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 요청 (브랜드명 빈 값)"),
            SwaggerApiResponse(responseCode = "401", description = "어드민 인증 실패"),
            SwaggerApiResponse(responseCode = "409", description = "이미 존재하는 브랜드명"),
        ],
    )
    fun createBrand(
        @Parameter(description = "어드민 LDAP 인증", required = true)
        ldap: String?,
        request: BrandV1Dto.CreateRequest,
    ): ApiResponse<BrandV1Dto.BrandAdminResponse>

    @Operation(summary = "브랜드 수정", description = "브랜드 정보를 수정합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "수정 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 요청 (브랜드명 빈 값)"),
            SwaggerApiResponse(responseCode = "401", description = "어드민 인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "브랜드 없음"),
            SwaggerApiResponse(responseCode = "409", description = "이미 존재하는 브랜드명"),
        ],
    )
    fun updateBrand(
        @Parameter(description = "어드민 LDAP 인증", required = true)
        ldap: String?,
        @Parameter(description = "브랜드 ID", required = true)
        brandId: Long,
        request: BrandV1Dto.UpdateRequest,
    ): ApiResponse<BrandV1Dto.BrandAdminResponse>

    @Operation(summary = "브랜드 삭제", description = "브랜드를 삭제합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "삭제 성공"),
            SwaggerApiResponse(responseCode = "401", description = "어드민 인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "브랜드 없음"),
        ],
    )
    fun deleteBrand(
        @Parameter(description = "어드민 LDAP 인증", required = true)
        ldap: String?,
        @Parameter(description = "브랜드 ID", required = true)
        brandId: Long,
    ): ApiResponse<Any>
}
