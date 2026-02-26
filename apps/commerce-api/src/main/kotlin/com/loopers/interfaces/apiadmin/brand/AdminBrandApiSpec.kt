package com.loopers.interfaces.apiadmin.brand

import com.loopers.interfaces.common.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Admin Brand API", description = "어드민 브랜드 API")
interface AdminBrandApiSpec {
    @Operation(
        summary = "브랜드 등록",
        description = "새로운 브랜드를 등록합니다.",
    )
    fun createBrand(request: AdminBrandDto.CreateRequest): ApiResponse<AdminBrandDto.CreateResponse>
}
