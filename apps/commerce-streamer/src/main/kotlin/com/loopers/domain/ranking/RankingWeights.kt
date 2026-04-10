package com.loopers.domain.ranking

/**
 * 랭킹 점수 계산에 사용되는 가중치 VO.
 *
 * Redis에 저장되어 런타임에 동적으로 변경 가능하다.
 * Redis 조회 실패 시 `DEFAULT` 값이 사용된다.
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
         * view: 0.1, like: 0.2, order: 0.7 (합계 1.0)
         */
        val DEFAULT = RankingWeights(
            view = 0.1,
            like = 0.2,
            order = 0.7,
        )
    }
}
