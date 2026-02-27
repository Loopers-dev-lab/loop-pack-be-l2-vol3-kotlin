package com.loopers.interfaces.api.brand.spec

import com.loopers.interfaces.api.brand.dto.BrandV1Dto
import com.loopers.interfaces.support.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Positive

@Tag(name = "Brand V1 API", description = "브랜드 조회 API")
interface BrandV1ApiSpec {

    @Operation(summary = "브랜드 조회", description = "브랜드를 조회합니다.")
    fun getBrand(@Positive brandId: Long): ApiResponse<BrandV1Dto.BrandResponse>
}
