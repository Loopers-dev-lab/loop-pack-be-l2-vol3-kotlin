package com.loopers.interfaces.api.user.ranking

import com.loopers.application.user.ranking.UserRankingListUseCase
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
        pageRequest: PageRequest,
    ): ApiResponse<PageResponse<UserRankingV1Response.RankedProduct>> {
        val parsedDate = parseDate(date)
        return listUseCase.getList(parsedDate, pageRequest)
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
}
