package com.loopers.batch.job.ranking

/**
 * 주간/월간 랭킹 집계 결과 단위. Reader가 SQL GROUP BY 결과를 이 형태로 반환하고,
 * Processor가 rank 번호를 부여해 MV Entity로 변환한다.
 *
 * `totalScore`는 SQL 측에서 `SUM(view)*0.1 + SUM(like)*0.2 + SUM(order_score)`로
 * 미리 계산되며, Processor/Writer는 이 값을 그대로 사용한다.
 */
data class AggregatedProductMetric(
    val productId: Long,
    val viewCount: Int,
    val likeCount: Int,
    val unitsSold: Int,
    val salesAmount: Long,
    val orderScore: Double,
    val totalScore: Double,
)
