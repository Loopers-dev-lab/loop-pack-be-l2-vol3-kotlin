package com.loopers.interfaces.api.ranking

import com.loopers.domain.ranking.Period
import com.loopers.interfaces.common.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Ranking API", description = "랭킹 API")
interface RankingApiSpec {
    @Operation(
        summary = "랭킹 조회",
        description = "일간/주간/월간 상품 랭킹을 페이지네이션하여 조회합니다.",
    )
    fun getRankings(date: String?, period: Period, page: Int, size: Int): ApiResponse<RankingDto.Response>
}
