package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingFacade
import com.loopers.interfaces.common.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
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
        validatePagination(page, size)
        return rankingFacade.getRankings(targetDate, page, size)
            .let { RankingDto.Response.from(it) }
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
}
