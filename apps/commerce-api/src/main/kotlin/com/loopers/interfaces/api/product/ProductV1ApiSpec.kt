package com.loopers.interfaces.api.product

import com.loopers.domain.product.dto.ProductInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Product V1 API", description = "상품 조회 및 좋아요 API")
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
        sort: String?,
    ): ApiResponse<PageResponse<ProductInfo>>

    @Operation(
        summary = "상품 좋아요 추가/취소",
        description = "특정 상품을 좋아요하거나 좋아요를 취소합니다",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "작업 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음"),
        ],
    )
    fun likeProduct(
        @Parameter(description = "로그인한 사용자 ID", required = true)
        userId: Long,
        @Parameter(description = "상품 ID", required = true)
        productId: Long,
    ): ApiResponse<Any>

    @Operation(
        summary = "상품 좋아요 취소",
        description = "특정 상품의 좋아요를 취소합니다",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 취소 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음"),
        ],
    )
    fun unlikeProduct(
        @Parameter(description = "로그인한 사용자 ID", required = true)
        userId: Long,
        @Parameter(description = "상품 ID", required = true)
        productId: Long,
    ): ApiResponse<Any>
}
