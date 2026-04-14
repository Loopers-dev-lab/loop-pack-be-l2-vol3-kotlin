package com.loopers.interfaces.api.ranking

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Ranking V1 API", description = "상품 랭킹 API")
interface RankingV1ApiSpec {
    @Operation(summary = "상품 랭킹 조회", description = "일별 상품 랭킹을 조회합니다.")
    fun getRanking(date: String, size: Int, page: Int): ApiResponse<PageResponse<RankingV1Dto.RankingResponse>>
}
