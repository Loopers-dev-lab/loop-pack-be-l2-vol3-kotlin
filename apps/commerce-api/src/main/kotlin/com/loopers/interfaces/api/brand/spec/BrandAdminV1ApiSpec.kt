package com.loopers.interfaces.api.brand.spec

import com.loopers.interfaces.api.brand.dto.BrandAdminV1Dto
import com.loopers.interfaces.support.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
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
    fun createBrand(@Valid request: BrandAdminV1Dto.CreateBrandRequest): ApiResponse<BrandAdminV1Dto.BrandAdminResponse>

    @Operation(summary = "브랜드 수정", description = "브랜드를 수정합니다.")
    fun updateBrand(
        brandId: Long,
        @Valid request: BrandAdminV1Dto.UpdateBrandRequest,
    ): ApiResponse<BrandAdminV1Dto.BrandAdminResponse>

    @Operation(summary = "브랜드 삭제", description = "브랜드를 삭제합니다.")
    fun deleteBrand(brandId: Long): ApiResponse<Any>
}
