package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingUseCase
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/rankings")
class RankingV1Controller(
    private val rankingUseCase: RankingUseCase,
) : RankingV1ApiSpec {
    @GetMapping
    override fun getPage(
        @RequestParam date: String,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "1") page: Int,
    ): ApiResponse<List<RankingV1Dto.RankedProductResponse>> {
        return rankingUseCase.getPage(date, size, page)
            .map(RankingV1Dto.RankedProductResponse::from)
            .let { ApiResponse.success(it) }
    }
}
