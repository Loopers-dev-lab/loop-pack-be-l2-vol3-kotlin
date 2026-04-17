package com.loopers.domain.ranking.fixture

import com.loopers.domain.ranking.RankingEntry
import com.loopers.domain.ranking.WeeklyRankingRepository
import java.time.LocalDate

class FakeWeeklyRankingRepository : WeeklyRankingRepository {

    private val store = HashMap<LocalDate, MutableMap<Long, Double>>()

    fun addScore(rankingDate: LocalDate, productId: Long, score: Double) {
        store.getOrPut(rankingDate) { mutableMapOf() }[productId] = score
    }

    override fun findRankings(rankingDate: LocalDate, page: Int, size: Int): List<RankingEntry> {
        val sorted = store[rankingDate]?.entries
            ?.sortedByDescending { it.value }
            ?: return emptyList()
        val offset = page * size
        return sorted.drop(offset).take(size).mapIndexed { index, entry ->
            RankingEntry(
                productId = entry.key,
                score = entry.value,
                rank = (offset + index).toLong(),
            )
        }
    }

    override fun countByRankingDate(rankingDate: LocalDate): Long {
        return store[rankingDate]?.size?.toLong() ?: 0L
    }
}
