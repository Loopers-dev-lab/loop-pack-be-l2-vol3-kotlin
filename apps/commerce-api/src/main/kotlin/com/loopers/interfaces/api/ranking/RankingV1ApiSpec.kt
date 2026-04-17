package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingPeriod
import com.loopers.interfaces.api.ApiResponse

interface RankingV1ApiSpec {
    fun getPage(date: String, period: RankingPeriod, size: Int, page: Int): ApiResponse<List<RankingV1Dto.RankedProductResponse>>
}
