package com.loopers.domain.ranking

import com.loopers.domain.ranking.model.MonthlyProductRank
import com.loopers.domain.ranking.repository.MonthlyRankingRepository

class FakeMonthlyRankingRepository : MonthlyRankingRepository {

    private val storage = mutableMapOf<String, MutableList<MonthlyProductRank>>()

    fun addEntry(entry: MonthlyProductRank) {
        storage.getOrPut(entry.periodKey) { mutableListOf() }.add(entry)
    }

    fun clear() {
        storage.clear()
    }

    override fun findAllByPeriodKey(periodKey: String): List<MonthlyProductRank> {
        return storage[periodKey]
            ?.sortedBy { it.rank }
            ?.take(100)
            ?: emptyList()
    }
}
