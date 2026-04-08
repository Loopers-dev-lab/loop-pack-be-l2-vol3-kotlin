package com.loopers.interfaces.api.ranking

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Ranking V1 API", description = "상품 랭킹 조회 API")
interface RankingV1ApiSpec {

    @Operation(
        summary = "일간 상품 랭킹 페이지 조회",
        description = "지정 일자(yyyyMMdd, 미지정 시 오늘)의 상품 랭킹을 페이징으로 조회합니다.",
    )
    fun getRankingPage(
        date: String?,
        page: Int,
        size: Int,
    ): ApiResponse<RankingV1Dto.RankingPageResponse>
}
