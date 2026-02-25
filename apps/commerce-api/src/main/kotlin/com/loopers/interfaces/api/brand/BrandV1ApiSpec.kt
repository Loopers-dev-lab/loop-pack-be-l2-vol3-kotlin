package com.loopers.interfaces.api.brand

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "Brand V1 API", description = "브랜드 API (대고객)")
interface BrandV1ApiSpec {

    @Operation(
        summary = "브랜드 조회",
        description = "브랜드 정보를 조회합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 브랜드"),
        ],
    )
    fun getBrand(brandId: Long): ApiResponse<BrandV1Dto.BrandResponse>
}
