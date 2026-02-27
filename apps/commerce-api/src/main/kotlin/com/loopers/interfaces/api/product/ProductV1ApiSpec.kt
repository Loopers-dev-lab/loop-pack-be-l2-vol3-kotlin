package com.loopers.interfaces.api.product

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Product V1 API", description = "대고객 상품 API")
interface ProductV1ApiSpec {

    @Operation(summary = "상품 목록 조회", description = "상품 목록을 페이징 조회합니다. brandId로 필터링할 수 있습니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    fun getAllProducts(
        @Parameter(description = "브랜드 ID (필터)")
        brandId: Long?,
        @ParameterObject pageable: Pageable,
    ): ApiResponse<Page<ProductV1Dto.ProductResponse>>

    @Operation(summary = "상품 상세 조회", description = "상품 ID로 상품을 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "404", description = "상품 없음"),
        ],
    )
    fun getProduct(
        @Parameter(description = "상품 ID", required = true)
        productId: Long,
    ): ApiResponse<ProductV1Dto.ProductResponse>
}
