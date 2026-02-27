package com.loopers.interfaces.api.brand

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Brand V1 API", description = "대고객 브랜드 API")
interface BrandV1ApiSpec {

    @Operation(summary = "브랜드 조회", description = "브랜드 ID로 브랜드를 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "404", description = "브랜드 없음"),
        ],
    )
    fun getBrand(
        @Parameter(description = "브랜드 ID", required = true)
        brandId: Long,
    ): ApiResponse<BrandV1Dto.BrandResponse>
}
