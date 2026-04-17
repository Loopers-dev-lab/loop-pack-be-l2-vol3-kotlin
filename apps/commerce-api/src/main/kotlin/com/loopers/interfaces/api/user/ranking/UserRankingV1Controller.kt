package com.loopers.interfaces.api.user.ranking

import com.loopers.application.user.ranking.UserRankingListUseCase
import com.loopers.application.user.ranking.RankingPeriod
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequestMapping("/api/v1/rankings")
@RestController
class UserRankingV1Controller(
    private val listUseCase: UserRankingListUseCase,
) : UserRankingV1ApiSpec {

    @GetMapping
    override fun getList(
        @RequestParam(required = false) date: String?,
        @RequestParam(required = false, name = "period") periodParam: String?,
        pageRequest: PageRequest,
    ): ApiResponse<PageResponse<UserRankingV1Response.RankedProduct>> {
        val parsedDate = parseDate(date)
        val resolvedPeriod = parsePeriod(periodParam)
        return listUseCase.getList(parsedDate, resolvedPeriod, pageRequest)
            .map { UserRankingV1Response.RankedProduct.from(it) }
            .let { ApiResponse.success(it) }
    }

    private fun parseDate(date: String?): LocalDate {
        if (date == null) {
            return LocalDate.now(ZoneId.of("Asia/Seoul"))
        }
        return runCatching {
            LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE)
        }.getOrElse {
            throw CoreException(ErrorType.BAD_REQUEST, "date 형식이 올바르지 않습니다 (yyyyMMdd)")
        }
    }

    /**
     * period 쿼리 파라미터를 RankingPeriod로 파싱한다.
     * - 파라미터 자체가 없으면 DAILY로 기본값 적용
     * - 파라미터가 있되 빈 문자열이거나 공백만 있으면 400 (Converter에 위임 시 Spring이 empty-string을 null로 bypass하므로 여기서 방어)
     * - 값이 있으면 대소문자 무관 변환, 실패 시 400
     */
    private fun parsePeriod(periodParam: String?): RankingPeriod {
        if (periodParam == null) {
            return RankingPeriod.DAILY
        }
        val trimmed = periodParam.trim()
        if (trimmed.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "period 파라미터가 비어 있다")
        }
        return runCatching { RankingPeriod.valueOf(trimmed.uppercase()) }
            .getOrElse {
                throw CoreException(ErrorType.BAD_REQUEST, "period 값이 올바르지 않습니다 (daily|weekly|monthly)")
            }
    }
}
