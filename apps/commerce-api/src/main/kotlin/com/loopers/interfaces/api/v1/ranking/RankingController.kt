package com.loopers.interfaces.api.v1.ranking

import com.loopers.application.ranking.GetRankingUseCase
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/v1/rankings")
class RankingController(
    private val getRankingUseCase: GetRankingUseCase,
) {
    @GetMapping
    fun getRankings(
        @RequestParam(required = false) date: String?,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @RequestParam(required = false, defaultValue = "1") page: Int,
    ): ApiResponse<GetRankingResponse> {
        val rankingDate = date ?: LocalDate.now().format(DATE_FORMATTER)
        val pageInfo = getRankingUseCase.getRankingPage(rankingDate, page, size)
        return ApiResponse.success(GetRankingResponse.from(pageInfo))
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
