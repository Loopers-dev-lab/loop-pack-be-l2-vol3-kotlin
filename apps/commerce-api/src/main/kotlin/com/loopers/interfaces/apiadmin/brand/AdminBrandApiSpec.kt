package com.loopers.interfaces.apiadmin.brand

import com.loopers.interfaces.common.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Admin Brand API", description = "어드민 브랜드 API")
interface AdminBrandApiSpec {
    @Operation(
        summary = "브랜드 목록 조회",
        description = "브랜드 목록을 페이징하여 조회합니다.",
    )
    fun getBrands(page: Int, size: Int): ApiResponse<AdminBrandDto.PageResponse>

    @Operation(
        summary = "브랜드 등록",
        description = "새로운 브랜드를 등록합니다.",
    )
    fun createBrand(request: AdminBrandDto.CreateRequest): ApiResponse<AdminBrandDto.CreateResponse>

    @Operation(
        summary = "브랜드 상세 조회",
        description = "브랜드 ID로 상세 정보를 조회합니다.",
    )
    fun getBrand(brandId: Long): ApiResponse<AdminBrandDto.DetailResponse>

    @Operation(
        summary = "브랜드 수정",
        description = "브랜드 정보를 수정합니다.",
    )
    fun updateBrand(brandId: Long, request: AdminBrandDto.UpdateRequest): ApiResponse<AdminBrandDto.DetailResponse>

    @Operation(
        summary = "브랜드 삭제",
        description = "브랜드를 삭제합니다. 소속 상품도 연쇄 삭제됩니다.",
    )
    fun deleteBrand(brandId: Long): ApiResponse<Any>
}
