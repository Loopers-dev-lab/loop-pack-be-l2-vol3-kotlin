package com.loopers.interfaces.api.product

import com.loopers.interfaces.support.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page

@Tag(name = "Product Admin V1 API", description = "상품 어드민 API")
interface ProductAdminV1ApiSpec {

    @Operation(summary = "상품 목록 조회", description = "상품 목록을 조회합니다.")
    fun getProducts(page: Int, size: Int): ApiResponse<Page<ProductAdminV1Dto.AdminProductResponse>>

    @Operation(summary = "상품 상세 조회", description = "상품을 상세 조회합니다.")
    fun getProduct(productId: Long): ApiResponse<ProductAdminV1Dto.AdminProductResponse>

    @Operation(summary = "상품 생성", description = "상품을 생성합니다.")
    fun createProduct(request: ProductAdminV1Dto.CreateProductRequest): ApiResponse<ProductAdminV1Dto.AdminProductResponse>

    @Operation(summary = "상품 수정", description = "상품을 수정합니다.")
    fun updateProduct(
        productId: Long,
        request: ProductAdminV1Dto.UpdateProductRequest,
    ): ApiResponse<ProductAdminV1Dto.AdminProductResponse>

    @Operation(summary = "상품 삭제", description = "상품을 삭제합니다.")
    fun deleteProduct(productId: Long): ApiResponse<Any>
}
