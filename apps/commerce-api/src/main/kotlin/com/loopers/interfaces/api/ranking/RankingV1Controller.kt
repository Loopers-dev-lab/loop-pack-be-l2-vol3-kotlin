package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingService
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
    private val rankingService: RankingService,
) : RankingV1ApiSpec {

    @GetMapping
    override fun getRankings(
        @RequestParam(required = false) date: String?,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "1") page: Int,
    ): ApiResponse<RankingV1Dto.RankingPageResponse> {
        val targetDate = date?.let { LocalDate.parse(it, DATE_FORMAT) } ?: LocalDate.now()
        val dateStr = targetDate.format(DATE_FORMAT)

        val pageInfo = rankingService.getRankings(targetDate, page, size)

        return ApiResponse.success(
            RankingV1Dto.RankingPageResponse.from(pageInfo, dateStr, page, size),
        )
    }

    companion object {
        private val DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE
    }
}
