package com.loopers.interfaces.api.product

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Product V1 API", description = "상품 API")
interface ProductV1ApiSpec {
    @Operation(summary = "상품 목록 조회", description = "상품 목록을 필터링/정렬하여 조회합니다.")
    fun getProducts(brandId: Long?, sort: String, page: Int, size: Int): ApiResponse<PageResponse<ProductV1Dto.ProductResponse>>

    @Operation(summary = "상품 정보 조회", description = "상품 ID로 상품 정보를 조회합니다.")
    fun getProduct(productId: Long): ApiResponse<ProductV1Dto.ProductResponse>
}
