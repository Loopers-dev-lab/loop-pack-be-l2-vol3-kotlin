package com.loopers.domain.ranking

/**
 * 랭킹 가중치 Fake (테스트용).
 */
class FakeRankingWeightsRepository : RankingWeightsRepository {

    private var current: RankingWeights? = null

    override fun findCurrent(): RankingWeights? = current

    fun setWeights(weights: RankingWeights) {
        current = weights
    }

    fun clear() {
        current = null
    }
}
