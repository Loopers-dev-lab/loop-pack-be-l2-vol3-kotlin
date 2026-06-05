package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingFacade
import com.loopers.domain.ranking.RankingPeriod
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import kotlin.math.ceil

@RestController
@RequestMapping("/api/v1/ranking")
class RankingV1Controller(
    private val rankingFacade: RankingFacade,
) : RankingV1ApiSpec {

    @GetMapping
    override fun getRanking(
        @RequestParam(defaultValue = "DAILY") period: String,
        @RequestParam date: String,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "1") page: Int,
    ): ApiResponse<PageResponse<RankingV1Dto.RankingResponse>> {
        val rankingPeriod = RankingPeriod.valueOf(period.uppercase())
        val content = rankingFacade.getRanking(rankingPeriod, date, page, size)
            .map { RankingV1Dto.RankingResponse.from(it) }
        val totalElements = rankingFacade.getRankingTotalCount(rankingPeriod, date)
        val totalPages = if (totalElements == 0L) 0 else ceil(totalElements.toDouble() / size).toInt()

        return PageResponse(
            content = content,
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
        ).let { ApiResponse.success(it) }
    }
}
