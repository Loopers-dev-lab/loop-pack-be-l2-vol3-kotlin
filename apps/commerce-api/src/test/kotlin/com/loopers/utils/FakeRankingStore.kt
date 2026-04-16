package com.loopers.utils

import com.loopers.application.ranking.RankingStore
import com.loopers.domain.ranking.RankingEntry

class FakeRankingStore : RankingStore {
    private val data = mutableMapOf<String, MutableMap<Long, Double>>()

    override fun getTopProducts(key: String, offset: Long, count: Long): List<RankingEntry> {
        val entries = data[key] ?: return emptyList()
        return entries.entries
            .sortedByDescending { it.value }
            .drop(offset.toInt())
            .take(count.toInt())
            .map { RankingEntry(it.key, it.value) }
    }

    override fun getTotalCount(key: String): Long {
        return data[key]?.size?.toLong() ?: 0L
    }

    override fun getRank(key: String, productId: Long): Long? {
        val entries = data[key] ?: return null
        val sorted = entries.entries.sortedByDescending { it.value }
        val index = sorted.indexOfFirst { it.key == productId }
        return if (index >= 0) index.toLong() else null
    }

    override fun getScore(key: String, productId: Long): Double? {
        return data[key]?.get(productId)
    }

    fun addScore(key: String, productId: Long, score: Double) {
        data.getOrPut(key) { mutableMapOf() }.merge(productId, score, Double::plus)
    }
}
