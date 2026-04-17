package com.loopers.interfaces.api.ranking

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Ranking V1 API", description = "상품 랭킹 조회 API 입니다.")
interface RankingV1ApiSpec {
    @Operation(
        summary = "상품 랭킹 조회",
        description = "지정 기간의 상품 랭킹을 페이징으로 조회합니다. 가중치 합산 score 기준 내림차순. period: daily(기본)|weekly|monthly",
    )
    fun getRanking(
        date: String?,
        page: Int,
        size: Int,
        period: String,
    ): ApiResponse<RankingV1Dto.RankingPageResponse>
}
