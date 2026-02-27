package com.loopers.interfaces.api.admin.product

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Admin Product V1 API", description = "어드민 상품 API")
interface AdminProductV1ApiSpec {
    @Operation(summary = "상품 목록 조회", description = "등록된 상품 목록을 페이지네이션하여 조회합니다.")
    fun getProducts(page: Int, size: Int, brandId: Long?): ApiResponse<PageResponse<AdminProductV1Dto.ProductResponse>>

    @Operation(summary = "상품 상세 조회", description = "상품 ID로 상품 상세 정보를 조회합니다.")
    fun getProduct(productId: Long): ApiResponse<AdminProductV1Dto.ProductResponse>

    @Operation(summary = "상품 등록", description = "새로운 상품을 등록합니다.")
    fun createProduct(req: AdminProductV1Dto.CreateProductRequest): ApiResponse<AdminProductV1Dto.ProductResponse>

    @Operation(summary = "상품 수정", description = "상품 정보를 수정합니다. 브랜드는 변경할 수 없습니다.")
    fun updateProduct(productId: Long, req: AdminProductV1Dto.UpdateProductRequest): ApiResponse<AdminProductV1Dto.ProductResponse>

    @Operation(summary = "상품 삭제", description = "상품을 삭제합니다.")
    fun deleteProduct(productId: Long): ApiResponse<Any>
}
