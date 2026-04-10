package com.loopers.domain.ranking

/**
 * 랭킹 점수 계산기 (순수 Domain Service).
 *
 * Repository 주입 없이 순수 객체 협력만 수행한다.
 * product_metrics의 집계값과 가중치를 기반으로 가중합산 점수를 계산한다.
 *
 * score = (viewCount * weights.view) + (likeCount * weights.like) + (orderCount * weights.order)
 *
 * 가중치는 `RankingWeights` VO로 주입받아 런타임 변경에 대응한다.
 */
object RankingScoreCalculator {

    fun calculate(
        viewCount: Long,
        likeCount: Long,
        orderCount: Long,
        weights: RankingWeights,
    ): Double {
        return (viewCount * weights.view) +
            (likeCount * weights.like) +
            (orderCount * weights.order)
    }
}
