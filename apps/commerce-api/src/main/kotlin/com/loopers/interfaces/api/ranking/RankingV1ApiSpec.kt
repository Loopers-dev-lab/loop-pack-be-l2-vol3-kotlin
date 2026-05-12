package com.loopers.interfaces.api.ranking

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Ranking V1 API", description = "랭킹 조회 API")
interface RankingV1ApiSpec {
    @Operation(
        summary = "랭킹 조회",
        description = "실시간/일간/주간/월간 rolling 랭킹과 확정 일/주/월 랭킹을 조회합니다.",
    )
    fun getRankings(
        type: String,
        page: Int,
        size: Int,
        date: String?,
        weekStartDate: String?,
        yearMonth: String?,
    ): ApiResponse<RankingV1Dto.RankingPageResponse>
}
