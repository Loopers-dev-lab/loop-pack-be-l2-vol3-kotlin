package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.GetRankingsUseCase
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.PageResult
import com.loopers.support.constant.ApiPaths
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping(ApiPaths.Rankings.BASE)
class RankingV1Controller(
    private val getRankingsUseCase: GetRankingsUseCase,
) {

    @GetMapping
    fun getRankings(
        @RequestParam(required = false) date: String?,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "0") page: Int,
    ): ApiResponse<PageResult<RankingResponse>> {
        val targetDate = date?.let { LocalDate.parse(it, DATE_FORMATTER) } ?: LocalDate.now()
        val result = getRankingsUseCase.execute(targetDate, page, size)
        val response = PageResult.of(
            content = result.content.map { RankingResponse.from(it) },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
        )
        return ApiResponse.success(response)
    }

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
