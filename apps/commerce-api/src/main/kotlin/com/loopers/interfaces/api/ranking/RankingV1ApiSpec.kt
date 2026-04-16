package com.loopers.interfaces.api.ranking

import com.loopers.interfaces.api.ApiResponse

interface RankingV1ApiSpec {
    fun getPage(date: String, size: Int, page: Int): ApiResponse<List<RankingV1Dto.RankedProductResponse>>
}
