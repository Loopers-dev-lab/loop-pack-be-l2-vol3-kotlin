package com.loopers.domain.ranking.fixture

import com.loopers.domain.ranking.RankingEntry
import com.loopers.domain.ranking.RankingRepository

class FakeRankingRepository : RankingRepository {

    // date -> (productId -> score)
    private val store = HashMap<String, MutableMap<Long, Double>>()

    fun addScore(date: String, productId: Long, score: Double) {
        store.getOrPut(date) { mutableMapOf() }
            .merge(productId, score) { old, new -> old + new }
    }

    override fun getTopRankings(date: String, offset: Long, size: Long): List<RankingEntry> {
        val sorted = store[date]?.entries
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

    override fun getRank(date: String, productId: Long): Long? {
        val sorted = store[date]?.entries
            ?.sortedByDescending { it.value }
            ?: return null
        val index = sorted.indexOfFirst { it.key == productId }
        return if (index >= 0) index.toLong() else null
    }

    override fun getScore(date: String, productId: Long): Double? {
        return store[date]?.get(productId)
    }

    override fun getTotalCount(date: String): Long {
        return store[date]?.size?.toLong() ?: 0L
    }
}
