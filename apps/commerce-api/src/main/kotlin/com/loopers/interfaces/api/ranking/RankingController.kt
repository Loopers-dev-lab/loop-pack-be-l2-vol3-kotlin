package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingFacade
import com.loopers.interfaces.common.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/v1/rankings")
class RankingController(
    private val rankingFacade: RankingFacade,
) : RankingApiSpec {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    @GetMapping
    override fun getRankings(
        @RequestParam(required = false) date: String?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<RankingDto.Response> {
        val targetDate = parseDate(date)
        return rankingFacade.getRankings(targetDate, page, size)
            .let { RankingDto.Response.from(it) }
            .let { ApiResponse.success(it) }
    }

    private fun parseDate(date: String?): LocalDate {
        if (date == null) return LocalDate.now()
        return LocalDate.parse(date, DATE_FORMATTER)
    }
}
