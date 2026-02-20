package com.loopers.interfaces.api.brand.spec

import com.loopers.interfaces.api.brand.dto.BrandAdminV1Dto
import com.loopers.interfaces.support.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.data.domain.Page

@Tag(name = "Brand Admin V1 API", description = "브랜드 어드민 API")
interface BrandAdminV1ApiSpec {

    @Operation(summary = "브랜드 목록 조회", description = "브랜드 목록을 조회합니다.")
    fun getBrands(@PositiveOrZero page: Int, @Positive @Max(100) size: Int): ApiResponse<Page<BrandAdminV1Dto.BrandAdminResponse>>

    @Operation(summary = "브랜드 상세 조회", description = "브랜드를 상세 조회합니다.")
    fun getBrand(brandId: Long): ApiResponse<BrandAdminV1Dto.BrandAdminResponse>

    @Operation(summary = "브랜드 생성", description = "브랜드를 생성합니다.")
    fun createBrand(
        @Parameter(description = "브랜드명") @NotBlank(message = "브랜드명은 필수입니다.") name: String,
    ): ApiResponse<BrandAdminV1Dto.BrandAdminResponse>

    @Operation(summary = "브랜드 수정", description = "브랜드를 수정합니다.")
    fun updateBrand(
        brandId: Long,
        @Parameter(description = "브랜드명") @NotBlank(message = "브랜드명은 필수입니다.") name: String,
    ): ApiResponse<BrandAdminV1Dto.BrandAdminResponse>

    @Operation(summary = "브랜드 삭제", description = "브랜드를 삭제합니다.")
    fun deleteBrand(brandId: Long): ApiResponse<Any>
}
