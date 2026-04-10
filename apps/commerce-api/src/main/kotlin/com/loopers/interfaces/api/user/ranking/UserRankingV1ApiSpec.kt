package com.loopers.interfaces.api.user.ranking

import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "[User] Ranking V1 API", description = "[User] Ranking API 입니다.")
interface UserRankingV1ApiSpec {
    @Operation(
        summary = "랭킹 목록 조회",
        description = "날짜별 상품 랭킹 목록을 조회합니다. 점수 내림차순으로 정렬됩니다.",
    )
    fun getList(
        @Parameter(
            description = "조회 날짜 (yyyyMMdd 형식, 미제공 시 오늘 기준 Asia/Seoul)",
            example = "20260410",
        )
        date: String?,
        pageRequest: PageRequest,
    ): ApiResponse<PageResponse<UserRankingV1Response.RankedProduct>>
}
