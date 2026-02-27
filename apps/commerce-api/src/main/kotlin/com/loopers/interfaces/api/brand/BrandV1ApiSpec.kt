package com.loopers.interfaces.api.brand

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Brand V1 API", description = "브랜드 API")
interface BrandV1ApiSpec {
    @Operation(
        summary = "브랜드 정보 조회",
        description = "브랜드 ID로 브랜드 정보를 조회합니다.",
    )
    fun getBrand(brandId: Long): ApiResponse<BrandV1Dto.BrandResponse>
}
