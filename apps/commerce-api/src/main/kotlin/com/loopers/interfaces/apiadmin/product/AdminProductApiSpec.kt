package com.loopers.interfaces.apiadmin.product

import com.loopers.interfaces.common.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Admin Product API", description = "어드민 상품 API")
interface AdminProductApiSpec {
    @Operation(
        summary = "상품 목록 조회",
        description = "상품 목록을 페이징하여 조회합니다.",
    )
    fun getProducts(
        brandId: Long?,
        page: Int,
        size: Int,
    ): ApiResponse<AdminProductDto.PageResponse>

    @Operation(
        summary = "상품 등록",
        description = "새로운 상품을 등록합니다.",
    )
    fun createProduct(request: AdminProductDto.CreateRequest): ApiResponse<AdminProductDto.CreateResponse>

    @Operation(
        summary = "상품 상세 조회",
        description = "특정 상품의 상세 정보를 조회합니다.",
    )
    fun getProductDetail(productId: Long): ApiResponse<AdminProductDto.DetailResponse>

    @Operation(
        summary = "상품 수정",
        description = "특정 상품의 정보를 수정합니다.",
    )
    fun updateProduct(productId: Long, request: AdminProductDto.UpdateRequest): ApiResponse<AdminProductDto.DetailResponse>

    @Operation(
        summary = "상품 삭제",
        description = "특정 상품을 삭제합니다.",
    )
    fun deleteProduct(productId: Long): ApiResponse<Unit>
}
