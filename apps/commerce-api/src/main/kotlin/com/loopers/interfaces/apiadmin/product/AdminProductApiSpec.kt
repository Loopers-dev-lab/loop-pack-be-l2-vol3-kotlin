package com.loopers.interfaces.apiadmin.product

import com.loopers.interfaces.common.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Admin Product API", description = "어드민 상품 API")
interface AdminProductApiSpec {
    @Operation(
        summary = "상품 등록",
        description = "새로운 상품을 등록합니다.",
    )
    fun createProduct(request: AdminProductDto.CreateRequest): ApiResponse<AdminProductDto.CreateResponse>
}
