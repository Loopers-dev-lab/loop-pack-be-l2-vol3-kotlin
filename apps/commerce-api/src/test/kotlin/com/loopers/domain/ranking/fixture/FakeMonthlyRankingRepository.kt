package com.loopers.domain.ranking.fixture

import com.loopers.domain.ranking.MonthlyRankingRepository
import com.loopers.domain.ranking.RankingEntry
import java.time.LocalDate

class FakeMonthlyRankingRepository : MonthlyRankingRepository {

    private val store = HashMap<LocalDate, MutableMap<Long, Double>>()

    fun addScore(rankingDate: LocalDate, productId: Long, score: Double) {
        store.getOrPut(rankingDate) { mutableMapOf() }[productId] = score
    }

    override fun findRankings(rankingDate: LocalDate, offset: Long, size: Long): List<RankingEntry> {
        val sorted = store[rankingDate]?.entries
            ?.sortedByDescending { it.value }
            ?: return emptyList()
        return sorted.drop(offset.toInt()).take(size.toInt()).mapIndexed { index, entry ->
            RankingEntry(
                productId = entry.key,
                score = entry.value,
                rank = offset + index,
            )
        }
    }

    override fun countByRankingDate(rankingDate: LocalDate): Long {
        return store[rankingDate]?.size?.toLong() ?: 0L
    }
}
