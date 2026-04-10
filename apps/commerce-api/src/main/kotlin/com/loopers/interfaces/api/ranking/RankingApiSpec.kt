package com.loopers.interfaces.api.ranking

import com.loopers.interfaces.common.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Ranking API", description = "랭킹 API")
interface RankingApiSpec {
    @Operation(
        summary = "랭킹 조회",
        description = "일별 상품 랭킹을 페이지네이션하여 조회합니다.",
    )
    fun getRankings(date: String?, page: Int, size: Int): ApiResponse<RankingDto.Response>
}
