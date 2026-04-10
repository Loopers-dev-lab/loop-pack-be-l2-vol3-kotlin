package com.loopers.interfaces.api.ranking

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "Ranking V1 API", description = "랭킹 API (대고객)")
interface RankingV1ApiSpec {

    @Operation(summary = "일간 랭킹 조회", description = "일간 인기 상품 랭킹을 페이징하여 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    fun getRankings(
        request: RankingV1Dto.GetRankingsRequest,
    ): ApiResponse<RankingV1Dto.RankingPageResponse>
}
