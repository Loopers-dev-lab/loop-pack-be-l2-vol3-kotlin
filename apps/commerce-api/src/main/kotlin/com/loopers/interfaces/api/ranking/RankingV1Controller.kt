package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingFacade
import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("/api/v1/rankings")
class RankingV1Controller(
    private val rankingFacade: RankingFacade,
) : RankingV1ApiSpec {

    @GetMapping
    override fun getRankings(
        @RequestParam(required = false) date: String?,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "0") page: Int,
    ): ApiResponse<PageResponse<RankingV1Dto.RankingItemResponse>> {
        val validSize = validateSize(size)
        val validPage = validatePage(page)
        val targetDate = validateDate(date)
        val result = rankingFacade.getRankings(targetDate, validPage, validSize)
        return ApiResponse.success(
            PageResponse(
                content = result.items.map { RankingV1Dto.RankingItemResponse.from(it) },
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    @GetMapping("/hourly")
    override fun getHourlyRankings(
        @RequestParam(required = false) date: String?,
        @RequestParam(required = false) hour: String?,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "0") page: Int,
    ): ApiResponse<PageResponse<RankingV1Dto.RankingItemResponse>> {
        val validSize = validateSize(size)
        val validPage = validatePage(page)
        val targetDate = validateDate(date)
        val targetHour = validateHour(hour)
        val result = rankingFacade.getHourlyRankings(targetDate, targetHour, validPage, validSize)
        return ApiResponse.success(
            PageResponse(
                content = result.items.map { RankingV1Dto.RankingItemResponse.from(it) },
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    private fun validateSize(size: Int): Int = size.coerceIn(1, MAX_SIZE)

    private fun validatePage(page: Int): Int = page.coerceAtLeast(0)

    private fun validateDate(date: String?): String {
        if (date == null) return LocalDateTime.now().format(DAILY_FORMATTER)
        if (!date.matches(DATE_PATTERN)) {
            throw CoreException(ErrorType.BAD_REQUEST, "date는 yyyyMMdd 형식이어야 합니다.")
        }
        try {
            LocalDate.parse(date, DAILY_FORMATTER)
        } catch (e: DateTimeParseException) {
            throw CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 날짜입니다: $date")
        }
        return date
    }

    private fun validateHour(hour: String?): String {
        if (hour == null) return LocalDateTime.now().format(HOURLY_FORMATTER)
        if (!hour.matches(HOUR_PATTERN)) {
            throw CoreException(ErrorType.BAD_REQUEST, "hour는 00~23 형식이어야 합니다.")
        }
        return hour
    }

    companion object {
        private const val MAX_SIZE = 100
        private val DAILY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val HOURLY_FORMATTER = DateTimeFormatter.ofPattern("HH")
        private val DATE_PATTERN = Regex("\\d{8}")
        private val HOUR_PATTERN = Regex("([01]\\d|2[0-3])")
    }
}
