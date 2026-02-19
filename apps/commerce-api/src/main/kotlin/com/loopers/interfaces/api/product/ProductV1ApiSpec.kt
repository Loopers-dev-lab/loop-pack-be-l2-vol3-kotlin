package com.loopers.interfaces.api.product

import com.loopers.domain.product.dto.ProductInfo
import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page

@Tag(name = "Product V1 API", description = "상품 조회 API")
interface ProductV1ApiSpec {

    @Operation(
        summary = "상품 정보 조회",
        description = "상품 정보를 조회합니다",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상품 조회 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품이 존재하지 않음"),
        ],
    )
    fun getProductInfo(
        @Parameter(
            description = "상품 ID",
            required = true,
        )
        productId: Long,
    ): ApiResponse<ProductInfo>

    @Operation(
        summary = "상품 목록 조회",
        description = "상품 목록을 페이징 처리하여 조회합니다",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상품 목록 조회 성공"),
        ],
    )
    fun getProducts(
        @Parameter(
            description = "브랜드 ID (선택사항)",
            required = false,
        )
        brandId: Long?,
        page: Int,
        size: Int,
        sort: String,
    ): ApiResponse<Page<ProductInfo>>
}
