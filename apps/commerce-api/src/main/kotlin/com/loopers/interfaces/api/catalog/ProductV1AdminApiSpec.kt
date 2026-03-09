package com.loopers.interfaces.api.catalog

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "Product V1 Admin API", description = "상품 관련 어드민 API 입니다.")
interface ProductV1AdminApiSpec {
    @Operation(
        summary = "상품 등록",
        description = "새로운 상품을 등록합니다.",
    )
    @SwaggerResponse(responseCode = "201", description = "등록 성공")
    fun register(
        ldap: String,
        request: ProductV1AdminDto.RegisterRequest,
    )

    @Operation(
        summary = "상품 목록 조회",
        description = "페이지네이션으로 상품 목록을 조회합니다.",
    )
    @SwaggerResponse(responseCode = "200", description = "조회 성공")
    fun getProducts(
        ldap: String,
        page: Int,
        size: Int,
        brandId: Long?,
    ): ApiResponse<ProductV1AdminDto.ProductSliceResponse>

    @Operation(
        summary = "상품 상세 조회",
        description = "상품의 상세 정보를 조회합니다.",
    )
    @SwaggerResponse(responseCode = "200", description = "조회 성공")
    fun getProduct(
        ldap: String,
        productId: Long,
    ): ApiResponse<ProductV1AdminDto.ProductDetailResponse>

    @Operation(
        summary = "상품 수정",
        description = "상품 정보를 수정합니다.",
    )
    @SwaggerResponse(responseCode = "204", description = "수정 성공")
    fun modifyProduct(
        ldap: String,
        productId: Long,
        request: ProductV1AdminDto.UpdateRequest,
    )

    @Operation(
        summary = "상품 삭제",
        description = "상품을 삭제합니다.",
    )
    @SwaggerResponse(responseCode = "204", description = "삭제 성공")
    fun deleteProduct(
        ldap: String,
        productId: Long,
    )
}
