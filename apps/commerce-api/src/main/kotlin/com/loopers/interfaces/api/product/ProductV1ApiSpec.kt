package com.loopers.interfaces.api.product

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Product V1 API", description = "상품 API 입니다.")
interface ProductV1ApiSpec {
    @Operation(summary = "상품 목록 조회", description = "상품 목록을 커서 기반 페이징으로 조회합니다.")
    fun getProducts(
        brandId: Long?,
        sort: ProductSortRequest,
        size: Int,
        cursor: String?,
    ): ApiResponse<ProductV1Dto.ProductListResponse>

    @Operation(summary = "상품 상세 조회", description = "상품 상세 정보를 조회합니다.")
    fun getProduct(productId: Long): ApiResponse<ProductV1Dto.ProductResponse>
}
