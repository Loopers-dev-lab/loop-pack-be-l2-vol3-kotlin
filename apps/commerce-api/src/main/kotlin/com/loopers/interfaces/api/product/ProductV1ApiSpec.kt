package com.loopers.interfaces.api.product

import com.loopers.interfaces.support.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page

@Tag(name = "Product V1 API", description = "상품 조회 API")
interface ProductV1ApiSpec {

    @Operation(summary = "상품 목록 조회", description = "상품 목록을 조회합니다.")
    fun getProducts(
        brandId: Long?,
        sort: String,
        page: Int,
        size: Int,
    ): ApiResponse<Page<ProductV1Dto.CustomerProductResponse>>

    @Operation(summary = "상품 상세 조회", description = "상품을 상세 조회합니다.")
    fun getProduct(productId: Long): ApiResponse<ProductV1Dto.ProductDetailResponse>
}
