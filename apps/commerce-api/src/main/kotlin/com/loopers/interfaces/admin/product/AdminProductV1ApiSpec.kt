package com.loopers.interfaces.admin.product

import com.loopers.domain.product.dto.ProductInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.admin.product.AdminProductV1Dto.CreateProductRequest
import com.loopers.interfaces.admin.product.AdminProductV1Dto.UpdateProductRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page

@Tag(name = "Admin Product V1 API", description = "상품 관리 API")
interface AdminProductV1ApiSpec {

    @Operation(
        summary = "상품 정보 조회",
        description = "관리자가 상품 정보를 조회합니다",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "상품 조회 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "상품이 존재하지 않음",
            ),
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
        description = "관리자가 상품 목록을 페이징 처리하여 조회합니다",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "상품 목록 조회 성공",
            ),
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
    ): ApiResponse<Page<ProductInfo>>

    @Operation(
        summary = "상품 생성",
        description = "관리자가 새로운 상품을 생성합니다",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "상품 생성 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
            ),
        ],
    )
    fun createProduct(request: CreateProductRequest): ApiResponse<Long>

    @Operation(
        summary = "상품 수정",
        description = "관리자가 상품 정보를 수정합니다",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "상품 수정 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "상품이 존재하지 않음",
            ),
        ],
    )
    fun updateProduct(
        @Parameter(
            description = "상품 ID",
            required = true,
        )
        productId: Long,
        request: UpdateProductRequest,
    ): ApiResponse<Any>

    @Operation(
        summary = "상품 삭제",
        description = "관리자가 상품을 삭제합니다",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "상품 삭제 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "상품이 존재하지 않음",
            ),
        ],
    )
    fun deleteProduct(
        @Parameter(
            description = "상품 ID",
            required = true,
        )
        productId: Long,
    ): ApiResponse<Any>
}
