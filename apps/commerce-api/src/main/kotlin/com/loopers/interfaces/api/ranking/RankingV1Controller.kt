package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingFacade
import com.loopers.application.ranking.RankingPageInfo
import com.loopers.domain.ranking.RankingPeriod
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/v1/rankings")
class RankingV1Controller(
    private val rankingFacade: RankingFacade,
) {
    @GetMapping
    fun getRankings(
        @RequestParam(defaultValue = "DAILY") period: RankingPeriod,
        @RequestParam date: String,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "1") page: Int,
    ): ApiResponse<RankingPageInfo> {
        val parsedDate = LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE)
        return ApiResponse.success(rankingFacade.getRankings(period, parsedDate, page, size))
    }
}
