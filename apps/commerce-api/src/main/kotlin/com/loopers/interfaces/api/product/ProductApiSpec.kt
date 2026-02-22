package com.loopers.interfaces.api.product

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Product API", description = "상품 API")
interface ProductApiSpec {
    @Operation(
        summary = "상품 목록 조회",
        description = "상품 목록을 페이징하여 조회합니다.",
    )
    fun getProducts(brandId: Long?, sort: String, page: Int, size: Int): ApiResponse<ProductDto.PageResponse>

    @Operation(
        summary = "상품 상세 조회",
        description = "상품 ID로 상품 상세 정보를 조회합니다.",
    )
    fun getProduct(productId: Long): ApiResponse<ProductDto.DetailResponse>
}
