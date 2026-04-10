package com.loopers.domain.ranking

/**
 * 랭킹 ZSET 쓰기 Fake (테스트용).
 *
 * 일간/시간 키를 모두 동일한 store에 저장한다 (키 구분만 다름).
 */
class FakeRankingRepository : RankingRepository {

    private val store = mutableMapOf<String, MutableMap<Long, Double>>()

    override fun updateScores(dateKey: String, hourKey: String, scores: Map<Long, Double>) {
        store.getOrPut(dateKey) { mutableMapOf() }.putAll(scores)
        store.getOrPut(hourKey) { mutableMapOf() }.putAll(scores)
    }

    override fun carryOverScores(fromDateKey: String, toDateKey: String, weight: Double) {
        val source = store[fromDateKey] ?: return
        val target = mutableMapOf<Long, Double>()
        source.forEach { (productId, score) ->
            target[productId] = score * weight
        }
        store[toDateKey] = target
    }

    fun getScore(key: String, productId: Long): Double? {
        return store[key]?.get(productId)
    }

    fun getScores(key: String): Map<Long, Double> {
        return store[key] ?: emptyMap()
    }

    fun clear() {
        store.clear()
    }
}
