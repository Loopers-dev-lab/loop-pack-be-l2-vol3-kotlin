package com.loopers.interfaces.api.ranking

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Ranking V1 API", description = "랭킹 API")
interface RankingV1ApiSpec {

    @Operation(summary = "일간 랭킹 조회", description = "날짜별 인기 상품 랭킹을 조회합니다.")
    fun getRankings(
        @Parameter(description = "조회 날짜 (yyyyMMdd, 미지정 시 오늘)") date: String?,
        @Parameter(description = "페이지 크기") size: Int,
        @Parameter(description = "페이지 번호 (0-based)") page: Int,
    ): ApiResponse<PageResponse<RankingV1Dto.RankingItemResponse>>

    @Operation(summary = "시간 단위 랭킹 조회", description = "시간 단위 인기 상품 랭킹을 조회합니다.")
    fun getHourlyRankings(
        @Parameter(description = "조회 날짜 (yyyyMMdd, 미지정 시 오늘)") date: String?,
        @Parameter(description = "조회 시간 (HH, 미지정 시 현재 시간)") hour: String?,
        @Parameter(description = "페이지 크기") size: Int,
        @Parameter(description = "페이지 번호 (0-based)") page: Int,
    ): ApiResponse<PageResponse<RankingV1Dto.RankingItemResponse>>
}
