package com.loopers.interfaces.api.product

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.PathVariable

@Tag(name = "Product V1 API", description = "상품 조회 API")
interface ProductV1ApiSpec {

    @Operation(
        summary = "상품 목록 조회",
        description = "상품 목록을 페이지 단위로 조회합니다.",
    )
    fun getProducts(
        brandId: Long?,
        sortType: ProductSortType,
        pageable: Pageable,
    ): ApiResponse<Page<ProductV1Dto.ProductResponse>>

    @Operation(
        summary = "상품 상세 조회",
        description = "상품 ID로 상품 상세 정보를 조회합니다.",
    )
    fun getProductById(@PathVariable productId: Long): ApiResponse<ProductV1Dto.ProductResponse>
}
