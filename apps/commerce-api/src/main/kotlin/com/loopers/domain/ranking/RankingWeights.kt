package com.loopers.domain.ranking

/**
 * 랭킹 점수 계산에 사용되는 가중치 VO.
 *
 * commerce-api에서는 Admin API를 통한 쓰기/읽기가 모두 가능하다.
 * Redis에 JSON으로 저장된다.
 */
data class RankingWeights(
    val view: Double,
    val like: Double,
    val order: Double,
) {
    init {
        require(view >= 0.0) { "view 가중치는 0 이상이어야 합니다." }
        require(like >= 0.0) { "like 가중치는 0 이상이어야 합니다." }
        require(order >= 0.0) { "order 가중치는 0 이상이어야 합니다." }
    }

    companion object {
        /**
         * 과제 발제 기본값.
         */
        val DEFAULT = RankingWeights(
            view = 0.1,
            like = 0.2,
            order = 0.7,
        )
    }
}
