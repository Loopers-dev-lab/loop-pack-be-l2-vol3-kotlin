package com.loopers.interfaces.api.brand

import com.loopers.domain.brand.dto.BrandInfo
import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Brand V1 API", description = "브랜드 단건 조회 API")
interface BrandV1ApiSpec {

    @Operation(
        summary = "브랜드 정보 조회",
        description = "브랜드 정보를 조회 합니다",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "브랜드가 존재하지 않음"),
        ],
    )
    fun getBrandInfo(
        @Parameter(
            description = "브랜드 ID",
            required = true,
        )
        brandId: Long,
    ): ApiResponse<BrandInfo>
}
