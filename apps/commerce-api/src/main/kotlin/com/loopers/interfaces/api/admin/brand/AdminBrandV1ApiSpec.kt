package com.loopers.interfaces.api.admin.brand

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Admin Brand V1 API", description = "어드민 브랜드 API")
interface AdminBrandV1ApiSpec {
    @Operation(summary = "브랜드 목록 조회", description = "등록된 브랜드 목록을 페이지네이션하여 조회합니다.")
    fun getBrands(page: Int, size: Int): ApiResponse<PageResponse<AdminBrandV1Dto.BrandResponse>>

    @Operation(summary = "브랜드 상세 조회", description = "브랜드 ID로 브랜드 상세 정보를 조회합니다.")
    fun getBrand(brandId: Long): ApiResponse<AdminBrandV1Dto.BrandResponse>

    @Operation(summary = "브랜드 등록", description = "새로운 브랜드를 등록합니다.")
    fun createBrand(req: AdminBrandV1Dto.CreateBrandRequest): ApiResponse<AdminBrandV1Dto.BrandResponse>

    @Operation(summary = "브랜드 수정", description = "브랜드 정보를 수정합니다.")
    fun updateBrand(brandId: Long, req: AdminBrandV1Dto.UpdateBrandRequest): ApiResponse<AdminBrandV1Dto.BrandResponse>

    @Operation(summary = "브랜드 삭제", description = "브랜드를 삭제합니다. 해당 브랜드의 상품들도 함께 삭제됩니다.")
    fun deleteBrand(brandId: Long): ApiResponse<Any>
}
