package com.loopers.application.ranking

import com.loopers.domain.ranking.MvRankingRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import kotlin.math.ceil

@Component
class MvRankingStore(
    @Qualifier("weekly") private val weekly: MvRankingRepository,
    @Qualifier("monthly") private val monthly: MvRankingRepository,
) {
    fun getRankings(period: RankingPeriod, periodKey: String, page: Int, size: Int): RankingPageResult {
        val repo = if (period == RankingPeriod.WEEKLY) weekly else monthly
        val entries = repo.findTop(periodKey, page, size)
        val total = repo.count(periodKey)
        val ranked = entries.map { RankedEntry(rank = it.rank, score = it.score, productId = it.productId) }
        val totalPages = if (total == 0L) 0 else ceil(total.toDouble() / size).toInt()
        return RankingPageResult(entries = ranked, totalElements = total, totalPages = totalPages)
    }
}
