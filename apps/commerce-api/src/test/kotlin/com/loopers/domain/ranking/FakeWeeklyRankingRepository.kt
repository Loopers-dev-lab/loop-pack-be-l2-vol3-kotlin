package com.loopers.domain.ranking

import com.loopers.domain.ranking.model.WeeklyProductRank
import com.loopers.domain.ranking.repository.WeeklyRankingRepository

class FakeWeeklyRankingRepository : WeeklyRankingRepository {

    private val storage = mutableMapOf<String, MutableList<WeeklyProductRank>>()

    fun addEntry(entry: WeeklyProductRank) {
        storage.getOrPut(entry.periodKey) { mutableListOf() }.add(entry)
    }

    fun clear() {
        storage.clear()
    }

    override fun findAllByPeriodKey(periodKey: String): List<WeeklyProductRank> {
        return storage[periodKey]
            ?.sortedBy { it.rank }
            ?.take(100)
            ?: emptyList()
    }
}
