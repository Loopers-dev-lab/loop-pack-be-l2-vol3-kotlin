package com.loopers.interfaces.api.ranking

import com.loopers.domain.ranking.RankingPeriodType
import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Ranking V1 API", description = "대고객 랭킹 API")
interface RankingV1ApiSpec {

    @Operation(summary = "랭킹 조회", description = "일간/주간/월간 상품 랭킹을 페이징 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    fun getRankings(
        @Parameter(description = "조회 날짜 (yyyyMMdd)", example = "20260408")
        date: String?,
        @Parameter(description = "페이지 크기", example = "20")
        size: Int,
        @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
        page: Int,
        @Parameter(description = "조회 기간 (DAILY, WEEKLY, MONTHLY)", example = "DAILY")
        period: RankingPeriodType,
    ): ApiResponse<RankingV1Dto.RankingPageResponse>
}
