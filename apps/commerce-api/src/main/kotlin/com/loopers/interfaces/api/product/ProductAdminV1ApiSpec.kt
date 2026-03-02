package com.loopers.interfaces.api.product

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "Product Admin V1 API", description = "상품 관리자 API")
interface ProductAdminV1ApiSpec {

    @Operation(summary = "상품 목록 조회", description = "전체 상품 목록을 페이징하여 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    fun getProducts(request: ProductAdminV1Dto.GetProductsAdminRequest): ApiResponse<Page<ProductAdminV1Dto.ProductAdminResponse>>

    @Operation(summary = "상품 상세 조회", description = "상품 상세 정보를 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 상품"),
        ],
    )
    fun getProduct(productId: Long): ApiResponse<ProductAdminV1Dto.ProductAdminResponse>

    @Operation(summary = "상품 등록", description = "새로운 상품을 등록합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "등록 성공"),
            SwaggerResponse(responseCode = "400", description = "잘못된 요청"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 브랜드"),
        ],
    )
    fun createProduct(request: ProductAdminV1Dto.CreateRequest): ApiResponse<ProductAdminV1Dto.ProductAdminResponse>

    @Operation(summary = "상품 수정", description = "상품 정보를 수정합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "수정 성공"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 상품"),
        ],
    )
    fun updateProduct(
        productId: Long,
        request: ProductAdminV1Dto.UpdateRequest,
    ): ApiResponse<ProductAdminV1Dto.ProductAdminResponse>

    @Operation(summary = "상품 삭제", description = "상품을 삭제합니다. 관련 좋아요도 함께 삭제됩니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "삭제 성공"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 상품"),
        ],
    )
    fun deleteProduct(productId: Long): ApiResponse<Unit>
}
