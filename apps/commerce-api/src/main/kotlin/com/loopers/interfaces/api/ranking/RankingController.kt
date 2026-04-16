package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingFacade
import com.loopers.domain.ranking.Period
import com.loopers.domain.ranking.YearWeek
import com.loopers.interfaces.common.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.YearMonth
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
        @RequestParam(defaultValue = "DAILY") period: Period,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<RankingDto.Response> {
        val targetDate = parseDate(date)
        validatePagination(page, size)

        val rankings = rankingFacade.getRankings(targetDate, period, page, size)
        val (periodStart, periodEnd) = resolvePeriodRange(targetDate, period)

        return RankingDto.Response.from(rankings, period, periodStart, periodEnd)
            .let { ApiResponse.success(it) }
    }

    private fun parseDate(date: String?): LocalDate {
        if (date == null) return LocalDate.now()
        return runCatching { LocalDate.parse(date, DATE_FORMATTER) }
            .getOrElse { throw CoreException(ErrorType.BAD_REQUEST, "날짜 형식이 올바르지 않습니다. (yyyyMMdd)") }
    }

    private fun validatePagination(page: Int, size: Int) {
        if (page < 1) throw CoreException(ErrorType.BAD_REQUEST, "page는 1 이상이어야 합니다.")
        if (size < 1 || size > 100) throw CoreException(ErrorType.BAD_REQUEST, "size는 1~100 사이여야 합니다.")
    }

    private fun resolvePeriodRange(date: LocalDate, period: Period): Pair<String, String> {
        return when (period) {
            Period.DAILY -> date.toString() to date.toString()
            Period.WEEKLY -> {
                val yearWeek = YearWeek.from(date)
                yearWeek.startDate.toString() to yearWeek.endDate.toString()
            }
            Period.MONTHLY -> {
                val yearMonth = YearMonth.from(date)
                yearMonth.atDay(1).toString() to yearMonth.atEndOfMonth().toString()
            }
        }
    }
}
