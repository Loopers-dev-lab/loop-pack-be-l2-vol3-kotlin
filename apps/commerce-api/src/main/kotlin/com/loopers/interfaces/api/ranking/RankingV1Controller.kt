package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.GetRankingUseCase
import com.loopers.interfaces.api.ranking.dto.RankingV1Dto
import com.loopers.interfaces.api.ranking.spec.RankingV1ApiSpec
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.toSpringPage
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Validated
@RestController
@RequestMapping("/api/v1/rankings")
class RankingV1Controller(
    private val getRankingUseCase: GetRankingUseCase,
) : RankingV1ApiSpec {

    @GetMapping
    override fun getRankings(
        @RequestParam(required = false) date: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<RankingV1Dto.RankingResponse>> {
        val parsedDate = date?.let { parseDate(it) }

        return getRankingUseCase.execute(parsedDate, page, size)
            .map { RankingV1Dto.RankingResponse.from(it) }
            .toSpringPage()
            .let { ApiResponse.success(it) }
    }

    private fun parseDate(date: String): LocalDate {
        try {
            return LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE)
        } catch (e: DateTimeParseException) {
            throw CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 날짜 형식입니다. yyyyMMdd 형식을 사용해주세요.")
        }
    }
}
