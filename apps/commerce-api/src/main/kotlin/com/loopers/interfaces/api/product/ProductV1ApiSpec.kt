package com.loopers.interfaces.api.product

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "Product V1 API", description = "상품 API (대고객)")
interface ProductV1ApiSpec {

    @Operation(summary = "상품 목록 조회", description = "판매중인 상품 목록을 페이징하여 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    fun getProducts(
        brandId: Long?,
        sort: String,
        page: Int,
        size: Int,
    ): ApiResponse<Page<ProductV1Dto.ProductResponse>>

    @Operation(summary = "상품 상세 조회", description = "상품 상세 정보를 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 상품"),
        ],
    )
    fun getProduct(productId: Long): ApiResponse<ProductV1Dto.ProductResponse>
}
