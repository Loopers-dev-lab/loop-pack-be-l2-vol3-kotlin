package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.GetRankingsCriteria
import com.loopers.application.ranking.RankingFacade
import com.loopers.interfaces.api.ApiResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/rankings")
class RankingV1Controller(
    private val rankingFacade: RankingFacade,
) : RankingV1ApiSpec {

    @GetMapping
    override fun getRankings(
        @ParameterObject request: RankingV1Dto.GetRankingsRequest,
    ): ApiResponse<RankingV1Dto.RankingPageResponse> {
        val criteria = GetRankingsCriteria(
            window = request.resolveWindow(),
            windowKey = request.resolveWindowKey(),
            page = request.page,
            size = request.size,
        )
        return rankingFacade.getRankings(criteria)
            .let { RankingV1Dto.RankingPageResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
