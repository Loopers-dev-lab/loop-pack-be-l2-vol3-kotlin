package com.loopers.interfaces.api.admin.brand

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page

@Tag(name = "Admin Brand V1 API", description = "어드민 브랜드 API 입니다.")
interface AdminBrandV1ApiSpec {
    @Operation(summary = "브랜드 등록", description = "새로운 브랜드를 등록합니다.")
    fun createBrand(request: AdminBrandV1Dto.CreateRequest): ApiResponse<AdminBrandV1Dto.BrandResponse>

    @Operation(summary = "브랜드 목록 조회", description = "브랜드 목록을 조회합니다.")
    fun getBrands(page: Int, size: Int): ApiResponse<Page<AdminBrandV1Dto.BrandResponse>>

    @Operation(summary = "브랜드 상세 조회", description = "브랜드 상세 정보를 조회합니다.")
    fun getBrand(brandId: Long): ApiResponse<AdminBrandV1Dto.BrandResponse>

    @Operation(summary = "브랜드 수정", description = "브랜드 정보를 수정합니다.")
    fun updateBrand(brandId: Long, request: AdminBrandV1Dto.UpdateRequest): ApiResponse<AdminBrandV1Dto.BrandResponse>

    @Operation(summary = "브랜드 삭제", description = "브랜드를 삭제합니다.")
    fun deleteBrand(brandId: Long): ApiResponse<Any>
}
