package com.loopers.interfaces.api.brand

import com.loopers.interfaces.common.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Brand API", description = "브랜드 API")
interface BrandApiSpec {
    @Operation(
        summary = "브랜드 상세 조회",
        description = "브랜드 ID로 브랜드 정보를 조회합니다.",
    )
    fun getBrand(brandId: Long): ApiResponse<BrandDto.DetailResponse>
}
