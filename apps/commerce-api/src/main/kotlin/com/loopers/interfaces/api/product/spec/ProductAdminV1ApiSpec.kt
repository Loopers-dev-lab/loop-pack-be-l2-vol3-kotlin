package com.loopers.interfaces.api.product.spec

import com.loopers.interfaces.api.product.dto.ProductAdminV1Dto
import com.loopers.interfaces.support.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.data.domain.Page

@Tag(name = "Product Admin V1 API", description = "상품 어드민 API")
interface ProductAdminV1ApiSpec {

    @Operation(summary = "상품 목록 조회", description = "상품 목록을 조회합니다.")
    fun getProducts(
        @PositiveOrZero page: Int,
        @Positive @Max(100) size: Int,
    ): ApiResponse<Page<ProductAdminV1Dto.AdminProductResponse>>

    @Operation(summary = "상품 상세 조회", description = "상품을 상세 조회합니다.")
    fun getProduct(@Positive productId: Long): ApiResponse<ProductAdminV1Dto.AdminProductResponse>

    @Operation(summary = "상품 생성", description = "상품을 생성합니다.")
    fun createProduct(@Valid request: ProductAdminV1Dto.CreateProductRequest): ApiResponse<ProductAdminV1Dto.AdminProductResponse>

    @Operation(summary = "상품 수정", description = "상품을 수정합니다.")
    fun updateProduct(
        @Positive productId: Long,
        @Valid request: ProductAdminV1Dto.UpdateProductRequest,
    ): ApiResponse<ProductAdminV1Dto.AdminProductResponse>

    @Operation(summary = "상품 삭제", description = "상품을 삭제합니다.")
    fun deleteProduct(@Positive productId: Long): ApiResponse<Any>

    @Operation(summary = "상품 복구", description = "삭제된 상품을 복구합니다.")
    fun restoreProduct(@Positive productId: Long): ApiResponse<ProductAdminV1Dto.AdminProductResponse>
}
