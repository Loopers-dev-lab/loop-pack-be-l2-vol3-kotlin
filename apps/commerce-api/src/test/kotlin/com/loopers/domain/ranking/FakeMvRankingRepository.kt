package com.loopers.domain.ranking

/**
 * MV 랭킹 조회 Fake (테스트용).
 *
 * 주간/월간 MV 데이터를 인메모리에서 관리한다.
 */
class FakeMvRankingRepository : MvRankingRepository {

    private val store = mutableMapOf<String, MutableList<RankingEntry>>()

    fun addRanking(window: RankingWindow, periodKey: String, productId: Long, score: Double) {
        val key = internalKey(window, periodKey)
        store.getOrPut(key) { mutableListOf() }
            .add(RankingEntry(productId = productId, score = score))
    }

    override fun getTopRankings(
        window: RankingWindow,
        periodKey: String,
        offset: Long,
        limit: Long,
    ): List<RankingEntry> {
        val entries = store[internalKey(window, periodKey)] ?: return emptyList()
        return entries
            .sortedByDescending { it.score }
            .drop(offset.toInt())
            .take(limit.toInt())
    }

    override fun getTotalCount(window: RankingWindow, periodKey: String): Long {
        return store[internalKey(window, periodKey)]?.size?.toLong() ?: 0L
    }

    fun clear() {
        store.clear()
    }

    private fun internalKey(window: RankingWindow, periodKey: String): String {
        return "${window.name}:$periodKey"
    }
}
