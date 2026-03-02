package com.loopers.interfaces.api.brand

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

@Tag(name = "Brand Admin V1 API", description = "브랜드 관리 Admin API")
interface BrandAdminV1ApiSpec {

    @Operation(
        summary = "브랜드 목록 조회",
        description = "브랜드 목록을 페이지 단위로 조회합니다.",
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
    fun getBrands(pageable: Pageable): ApiResponse<Page<BrandAdminV1Dto.BrandResponse>>

    @Operation(
        summary = "브랜드 상세 조회",
        description = "브랜드 ID로 브랜드 상세 정보(businessNumber 포함)를 조회합니다.",
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
    fun getBrandById(@PathVariable brandId: Long): ApiResponse<BrandAdminV1Dto.BrandResponse>

    @Operation(
        summary = "브랜드 등록",
        description = "새로운 브랜드를 등록합니다.",
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
    fun createBrand(@RequestBody request: BrandAdminV1Dto.CreateBrandRequest): ApiResponse<BrandAdminV1Dto.BrandResponse>

    @Operation(
        summary = "브랜드 정보 수정",
        description = "브랜드 ID로 브랜드 정보를 수정합니다.",
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
    fun updateBrand(
        @PathVariable brandId: Long,
        @RequestBody request: BrandAdminV1Dto.UpdateBrandRequest,
    ): ApiResponse<BrandAdminV1Dto.BrandResponse>

    @Operation(
        summary = "브랜드 삭제",
        description = "브랜드 ID로 브랜드를 삭제합니다.",
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
    fun deleteBrand(@PathVariable brandId: Long): ApiResponse<Unit>
}
