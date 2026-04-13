package com.loopers.interfaces.api.ranking.spec

import com.loopers.interfaces.api.ranking.dto.RankingV1Dto
import com.loopers.interfaces.support.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero

@Tag(name = "Ranking V1 API", description = "랭킹 조회 API")
interface RankingV1ApiSpec {

    @Operation(summary = "랭킹 조회", description = "실시간 상품 랭킹을 조회합니다.")
    fun getRankings(
        date: String?,
        @PositiveOrZero page: Int,
        @Positive @Max(100) size: Int,
    ): ApiResponse<RankingV1Dto.RankingPageResponse>
}
