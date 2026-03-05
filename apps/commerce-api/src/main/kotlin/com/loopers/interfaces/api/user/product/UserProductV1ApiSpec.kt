package com.loopers.interfaces.api.user.product

import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "[User] Product V1 API", description = "[User] Product API 입니다.")
interface UserProductV1ApiSpec {
    @Operation(summary = "상품 목록 조회", description = "활성 상태의 상품 목록을 조회합니다.")
    fun getList(
        pageRequest: PageRequest,
        brandId: Long?,
        sort: String?,
    ): ApiResponse<PageResponse<UserProductV1Response.Summary>>

    @Operation(summary = "상품 상세 조회", description = "상품 상세 정보를 조회합니다.")
    fun getDetail(productId: Long): ApiResponse<UserProductV1Response.Detail>
}
