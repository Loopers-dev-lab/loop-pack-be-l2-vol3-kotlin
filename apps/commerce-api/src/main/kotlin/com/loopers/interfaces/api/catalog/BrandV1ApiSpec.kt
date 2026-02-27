package com.loopers.interfaces.api.catalog

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Brand V1 API", description = "브랜드 관련 사용자 API 입니다.")
interface BrandV1ApiSpec {
    @Operation(
        summary = "브랜드 상세 조회",
        description = "브랜드의 상세 정보를 조회합니다.",
    )
    fun getBrand(
        loginId: String,
        loginPw: String,
        brandId: Long,
    ): ApiResponse<BrandV1Dto.BrandResponse>
}
