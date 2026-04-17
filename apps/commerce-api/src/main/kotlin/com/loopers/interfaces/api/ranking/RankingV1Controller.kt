package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.GetRankingCriteria
import com.loopers.application.ranking.UserGetRankingUseCase
import com.loopers.domain.ranking.RankingPeriod
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("/api/v1/rankings")
class RankingV1Controller(
    private val userGetRankingUseCase: UserGetRankingUseCase,
) : RankingV1ApiSpec {
    companion object {
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    @GetMapping
    override fun getRanking(
        @RequestParam(required = false) date: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "daily") period: String,
    ): ApiResponse<RankingV1Dto.RankingPageResponse> {
        if (page < 0) throw CoreException(ErrorType.BAD_REQUEST, "page는 0 이상이어야 합니다: $page")
        if (size < 1) throw CoreException(ErrorType.BAD_REQUEST, "size는 1 이상이어야 합니다: $size")

        val rankingPeriod = parsePeriod(period)
        val targetDate = parseDate(date) ?: LocalDate.now()
        val criteria = GetRankingCriteria(date = targetDate, page = page, size = size, period = rankingPeriod)
        return userGetRankingUseCase.execute(criteria)
            .let { RankingV1Dto.RankingPageResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    private fun parseDate(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) return null
        return try {
            LocalDate.parse(raw, DATE_FORMAT)
        } catch (e: DateTimeParseException) {
            throw CoreException(ErrorType.BAD_REQUEST, "date 파라미터 형식이 올바르지 않습니다: '$raw' (yyyyMMdd 필요)")
        }
    }

    private fun parsePeriod(raw: String): RankingPeriod {
        return try {
            RankingPeriod.valueOf(raw.uppercase())
        } catch (e: IllegalArgumentException) {
            throw CoreException(ErrorType.BAD_REQUEST, "period 파라미터가 올바르지 않습니다: '$raw' (daily|weekly|monthly)")
        }
    }
}
