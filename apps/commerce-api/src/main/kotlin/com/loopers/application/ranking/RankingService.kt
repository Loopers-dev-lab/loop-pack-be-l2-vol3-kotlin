package com.loopers.application.ranking

import com.loopers.domain.ranking.PeriodKeyResolver
import com.loopers.event.EventContract
import org.springframework.stereotype.Component
import java.time.LocalDate
import kotlin.math.ceil

@Component
class RankingService(
    private val rankingStore: RankingStore,
    private val mvRankingStore: MvRankingStore,
) {

    fun getTopRankings(date: String, page: Int, size: Int): RankingPageResult {
        val key = buildDailyKey(date)
        return fetchRankingPage(key, page, size)
    }

    fun getHourlyTopRankings(date: String, hour: String, page: Int, size: Int): RankingPageResult {
        val key = buildHourlyKey(date, hour)
        return fetchRankingPage(key, page, size)
    }

    fun getRank(date: String, productId: Long): RankingPosition? {
        val key = buildDailyKey(date)
        val rank = rankingStore.getRank(key, productId) ?: return null
        val score = rankingStore.getScore(key, productId) ?: 0.0
        return RankingPosition(rank = (rank + 1).toInt(), score = score)
    }

    private fun fetchRankingPage(key: String, page: Int, size: Int): RankingPageResult {
        val offset = (page * size).toLong()
        val entries = rankingStore.getTopProducts(key, offset, size.toLong())
        val totalElements = rankingStore.getTotalCount(key)
        val totalPages = if (totalElements == 0L) 0 else ceil(totalElements.toDouble() / size).toInt()

        val rankedEntries = entries.mapIndexed { index, entry ->
            RankedEntry(
                rank = (offset + index + 1).toInt(),
                score = entry.score,
                productId = entry.productId,
            )
        }
        return RankingPageResult(
            entries = rankedEntries,
            totalElements = totalElements,
            totalPages = totalPages,
        )
    }

    fun getPeriodRankings(period: RankingPeriod, targetDate: LocalDate, page: Int, size: Int): RankingPageResult {
        val periodKey = when (period) {
            RankingPeriod.WEEKLY -> PeriodKeyResolver.resolveWeekKey(targetDate)
            RankingPeriod.MONTHLY -> PeriodKeyResolver.resolveMonthKey(targetDate)
            RankingPeriod.DAILY -> error("Use getTopRankings for DAILY")
        }
        return mvRankingStore.getRankings(period, periodKey, page, size)
    }

    private fun buildDailyKey(date: String): String = "${EventContract.RANKING_KEY_PREFIX}:$date"

    private fun buildHourlyKey(date: String, hour: String): String = "${EventContract.RANKING_KEY_PREFIX}:$date:$hour"
}

data class RankingPageResult(
    val entries: List<RankedEntry>,
    val totalElements: Long,
    val totalPages: Int,
)
