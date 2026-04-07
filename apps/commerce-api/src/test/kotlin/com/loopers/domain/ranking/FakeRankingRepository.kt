package com.loopers.domain.ranking

import com.loopers.domain.ranking.model.RankingEntry
import com.loopers.domain.ranking.repository.RankingRepository
import java.time.LocalDate

class FakeRankingRepository : RankingRepository {

    private val entries = mutableMapOf<LocalDate, MutableMap<Long, Double>>()
    var shouldThrow: Boolean = false

    fun addEntry(date: LocalDate, productId: Long, score: Double) {
        entries.getOrPut(date) { mutableMapOf() }[productId] = score
    }

    fun clear() {
        entries.clear()
        shouldThrow = false
    }

    override fun getTopN(date: LocalDate, offset: Int, limit: Int): List<RankingEntry> {
        if (shouldThrow) throw RuntimeException("Redis 연결 실패")
        val sorted = sortedEntries(date).filter { it.second > 0 }
        return sorted.drop(offset).take(limit)
            .map { (productId, score) -> RankingEntry(productId = productId, score = score) }
    }

    override fun getRank(date: LocalDate, productId: Long): Int? {
        if (shouldThrow) throw RuntimeException("Redis 연결 실패")
        val sorted = sortedEntries(date).filter { it.second > 0 }
        val index = sorted.indexOfFirst { it.first == productId }
        return if (index >= 0) index + 1 else null
    }

    /**
     * Redis ZSET의 ZREVRANGE 동작 재현:
     * 내부 순서(score ASC, member lex ASC)를 전체 역순으로 반환하므로
     * score 내림차순, 동점 시 member(productId) 문자열 lexicographic 내림차순이 된다.
     */
    private fun sortedEntries(date: LocalDate): List<Pair<Long, Double>> =
        entries[date]?.entries
            ?.sortedWith(
                compareByDescending<Map.Entry<Long, Double>> { it.value }
                    .thenByDescending { it.key.toString() },
            )
            ?.map { it.key to it.value }
            ?: emptyList()
}
