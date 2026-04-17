package com.loopers.batch.job.ranking

/**
 * 주간/월간 랭킹 집계 결과 단위. Reader가 SQL GROUP BY 결과를 이 형태로 반환하고,
 * Processor는 `rankNumber`를 그대로 사용해 MV Entity로 변환한다.
 *
 * `totalScore`는 SQL 측에서 `SUM(view)*0.1 + SUM(like)*0.2 + SUM(order_score)`로
 * 미리 계산되며, `rankNumber`도 SQL `ROW_NUMBER()`로 부여된다. restart·병렬 안전성을
 * 위해 Processor에서 순번을 매기지 않고 SQL 결과를 그대로 신뢰한다.
 */
data class AggregatedProductMetric(
    val productId: Long,
    val viewCount: Int,
    val likeCount: Int,
    val unitsSold: Int,
    val salesAmount: Long,
    val orderScore: Double,
    val totalScore: Double,
    val rankNumber: Int,
)
