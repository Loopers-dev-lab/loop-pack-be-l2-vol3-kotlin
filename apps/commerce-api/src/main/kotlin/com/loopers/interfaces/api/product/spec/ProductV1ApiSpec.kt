package com.loopers.interfaces.api.product.spec

import com.loopers.interfaces.api.product.dto.ProductV1Dto
import com.loopers.interfaces.support.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.data.domain.Page

@Tag(name = "Product V1 API", description = "상품 조회 API")
interface ProductV1ApiSpec {

    @Operation(summary = "상품 목록 조회", description = "상품 목록을 조회합니다.")
    fun getProducts(
        brandId: Long?,
        sort: String,
        @PositiveOrZero page: Int,
        @Positive @Max(100) size: Int,
    ): ApiResponse<Page<ProductV1Dto.CustomerProductResponse>>

    @Operation(summary = "상품 상세 조회", description = "상품을 상세 조회합니다.")
    fun getProduct(productId: Long): ApiResponse<ProductV1Dto.ProductDetailResponse>
}
