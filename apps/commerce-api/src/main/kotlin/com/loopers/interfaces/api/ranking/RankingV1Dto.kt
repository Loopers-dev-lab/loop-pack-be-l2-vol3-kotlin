package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankedProductResult
import com.loopers.application.ranking.RankingPageResult
import com.loopers.domain.Money
import com.loopers.domain.ranking.RankingWindow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

class RankingV1Dto {

    data class GetRankingsRequest(
        val window: String? = null,
        val date: String? = null,
        val hour: String? = null,
        val week: String? = null,
        val month: String? = null,
        val page: Int = 0,
        val size: Int = 20,
    ) {
        companion object {
            private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
            private val HOUR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHH")
        }

        fun resolveWindow(): RankingWindow = RankingWindow.from(window)

        /**
         * window 파라미터에 맞춰 적절한 windowKey를 생성한다.
         * - DAILY: date 파라미터 우선, 없으면 오늘 날짜
         * - HOURLY: hour 파라미터 우선, 없으면 현재 시각
         * - WEEKLY: week 파라미터 우선 (ISO 형식 "2026-W15"), 없으면 현재 주차
         * - MONTHLY: month 파라미터 우선 ("2026-04"), 없으면 현재 월
         */
        fun resolveWindowKey(): String {
            val now = LocalDateTime.now()
            return when (resolveWindow()) {
                RankingWindow.DAILY -> date ?: now.format(DATE_FORMAT)
                RankingWindow.HOURLY -> hour ?: now.format(HOUR_FORMAT)
                RankingWindow.WEEKLY -> week ?: resolveCurrentWeekKey()
                RankingWindow.MONTHLY -> month ?: YearMonth.now().toString()
            }
        }

        private fun resolveCurrentWeekKey(): String {
            val today = LocalDate.now()
            val weekFields = WeekFields.of(Locale.getDefault())
            val weekNumber = today.get(weekFields.weekOfWeekBasedYear())
            val year = today.get(weekFields.weekBasedYear())
            return "$year-W${weekNumber.toString().padStart(2, '0')}"
        }
    }

    data class RankedProductResponse(
        val rank: Long,
        val score: Double,
        val productId: Long,
        val productName: String,
        val brandName: String,
        val price: Money,
        val imageUrl: String?,
        val soldOut: Boolean,
    ) {
        companion object {
            fun from(result: RankedProductResult): RankedProductResponse {
                return RankedProductResponse(
                    rank = result.rank,
                    score = result.score,
                    productId = result.productId,
                    productName = result.productName,
                    brandName = result.brandName,
                    price = result.price,
                    imageUrl = result.imageUrl,
                    soldOut = result.soldOut,
                )
            }
        }
    }

    data class RankingPageResponse(
        val rankings: List<RankedProductResponse>,
        val totalCount: Long,
        val page: Int,
        val size: Int,
    ) {
        companion object {
            fun from(result: RankingPageResult): RankingPageResponse {
                return RankingPageResponse(
                    rankings = result.rankings.map { RankedProductResponse.from(it) },
                    totalCount = result.totalCount,
                    page = result.page,
                    size = result.size,
                )
            }
        }
    }
}
