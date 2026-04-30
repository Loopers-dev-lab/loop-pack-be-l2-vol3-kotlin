package com.loopers.domain.ranking

/**
 * 랭킹 점수 계산기 (배치 모듈용).
 *
 * commerce-streamer의 동명 클래스와 동일한 로직이지만,
 * 모듈 독립성을 위해 별도 정의한다. (KEV 멘토: "중복되더라도 명확히 분리")
 *
 * score = (viewCount * weights.view) + (likeCount * weights.like) + (orderCount * weights.order)
 */
object RankingScoreCalculator {

    fun calculate(
        viewCount: Long,
        likeCount: Long,
        orderCount: Long,
        viewWeight: Double,
        likeWeight: Double,
        orderWeight: Double,
    ): Double {
        return (viewCount * viewWeight) +
            (likeCount * likeWeight) +
            (orderCount * orderWeight)
    }
}
