package com.loopers.interfaces.api.ranking

import com.loopers.domain.product.dto.ProductInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.LocalDate

@Tag(name = "Product Ranking V1 API", description = "상품 랭킹 조회 API")
interface ProductRankingV1ApiSpec {
    @Operation(
        summary = "상품 랭킹 조회",
        description = "Redis 기반 상품 랭킹을 페이지 단위로 조회합니다",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상품 랭킹 조회 성공"),
        ],
    )
    fun getRankedProducts(
        page: Int,
        size: Int,
        @Parameter(description = "KST 기준 처리 일자 (yyyyMMdd)", required = false)
        date: LocalDate?,
    ): ApiResponse<PageResponse<ProductInfo>>
}
