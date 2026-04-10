package com.loopers.interfaces.api.user.ranking

import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "[User] Ranking V1 API", description = "[User] Ranking API 입니다.")
interface UserRankingV1ApiSpec {
    @Operation(summary = "랭킹 목록 조회", description = "날짜별 상품 랭킹 목록을 조회합니다.")
    fun getList(
        date: String?,
        pageRequest: PageRequest,
    ): ApiResponse<PageResponse<UserRankingV1Response.RankedProduct>>
}
