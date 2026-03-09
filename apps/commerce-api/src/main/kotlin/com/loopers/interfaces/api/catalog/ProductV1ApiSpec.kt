package com.loopers.interfaces.api.catalog

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Product V1 API", description = "상품 관련 사용자 API 입니다.")
interface ProductV1ApiSpec {
    @Operation(
        summary = "상품 목록 조회",
        description = "페이지네이션으로 상품 목록을 조회합니다. 브랜드 필터 및 정렬을 지원합니다.",
    )
    fun getProducts(
        loginId: String,
        loginPw: String,
        page: Int,
        size: Int,
        brandId: Long?,
        sort: String,
    ): ApiResponse<ProductV1Dto.ProductSliceResponse>

    @Operation(
        summary = "상품 상세 조회",
        description = "상품의 상세 정보를 조회합니다.",
    )
    fun getProduct(
        loginId: String,
        loginPw: String,
        productId: Long,
    ): ApiResponse<ProductV1Dto.ProductDetailResponse>
}
