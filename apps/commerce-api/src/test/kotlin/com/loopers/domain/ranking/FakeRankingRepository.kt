package com.loopers.domain.ranking

/**
 * 랭킹 ZSET 읽기 Fake (테스트용).
 *
 * 일간/시간 윈도우를 같은 store에서 관리. Window와 windowKey의 조합을 내부 키로 사용.
 */
class FakeRankingRepository : RankingRepository {

    private val store = mutableMapOf<String, MutableMap<Long, Double>>()

    fun addScore(window: RankingWindow, windowKey: String, productId: Long, score: Double) {
        val internal = internalKey(window, windowKey)
        val zset = store.getOrPut(internal) { mutableMapOf() }
        zset[productId] = score
    }

    override fun getTopRankings(
        window: RankingWindow,
        windowKey: String,
        offset: Long,
        limit: Long,
    ): List<RankingEntry> {
        val zset = store[internalKey(window, windowKey)] ?: return emptyList()
        return zset.entries
            .sortedByDescending { it.value }
            .drop(offset.toInt())
            .take(limit.toInt())
            .map { RankingEntry(productId = it.key, score = it.value) }
    }

    override fun getRank(window: RankingWindow, windowKey: String, productId: Long): Long? {
        val zset = store[internalKey(window, windowKey)] ?: return null
        val sorted = zset.entries.sortedByDescending { it.value }
        val index = sorted.indexOfFirst { it.key == productId }
        return if (index >= 0) index.toLong() else null
    }

    override fun getScore(window: RankingWindow, windowKey: String, productId: Long): Double? {
        return store[internalKey(window, windowKey)]?.get(productId)
    }

    override fun getTotalCount(window: RankingWindow, windowKey: String): Long {
        return store[internalKey(window, windowKey)]?.size?.toLong() ?: 0L
    }

    fun clear() {
        store.clear()
    }

    private fun internalKey(window: RankingWindow, windowKey: String): String {
        return "${window.name}:$windowKey"
    }
}
