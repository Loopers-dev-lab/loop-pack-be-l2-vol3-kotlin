package com.loopers.domain.ranking

import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

@Component
class RankingService(
    private val rankingRepository: RankingRepository,
    private val periodicRankingRepository: PeriodicRankingRepository,
) {
    companion object {
        private val YEAR_MONTH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    }

    fun getTopRankings(period: RankingPeriod, date: LocalDate, page: Int, size: Int): RankingPage {
        val offset = ((page - 1) * size).toLong()
        return when (period) {
            RankingPeriod.DAILY -> getDailyTopRankings(date, offset, size, page)
            RankingPeriod.WEEKLY -> getWeeklyTopRankings(date, offset, size, page)
            RankingPeriod.MONTHLY -> getMonthlyTopRankings(date, offset, size, page)
        }
    }

    fun getProductRank(date: LocalDate, productId: Long): Long? {
        val key = RankingKeyGenerator.dailyKey(date)
        return rankingRepository.getRank(key, productId)?.let { it + 1 }
    }

    private fun getDailyTopRankings(date: LocalDate, offset: Long, size: Int, page: Int): RankingPage {
        val key = RankingKeyGenerator.dailyKey(date)
        val entries = rankingRepository.getTopN(key, offset, size.toLong())
        val totalCount = rankingRepository.getTotalCount(key)
        return RankingPage(
            entries = entries.mapIndexed { index, entry ->
                RankedProduct(
                    rank = offset + index + 1,
                    productId = entry.productId,
                    score = entry.score,
                )
            },
            totalElements = totalCount,
            page = page,
            size = size,
        )
    }

    private fun getWeeklyTopRankings(date: LocalDate, offset: Long, size: Int, page: Int): RankingPage {
        val periodStart = date.with(WeekFields.ISO.dayOfWeek(), 1)
        val entries = periodicRankingRepository.findTopWeekly(periodStart, offset, size.toLong())
        val totalCount = periodicRankingRepository.countWeekly(periodStart)
        return RankingPage(
            entries = entries,
            totalElements = totalCount,
            page = page,
            size = size,
        )
    }

    private fun getMonthlyTopRankings(date: LocalDate, offset: Long, size: Int, page: Int): RankingPage {
        val yearMonthVal = YearMonth.from(date).format(YEAR_MONTH_FORMATTER)
        val entries = periodicRankingRepository.findTopMonthly(yearMonthVal, offset, size.toLong())
        val totalCount = periodicRankingRepository.countMonthly(yearMonthVal)
        return RankingPage(
            entries = entries,
            totalElements = totalCount,
            page = page,
            size = size,
        )
    }
}

data class RankedProduct(
    val rank: Long,
    val productId: Long,
    val score: Double,
)

data class RankingPage(
    val entries: List<RankedProduct>,
    val totalElements: Long,
    val page: Int,
    val size: Int,
)
