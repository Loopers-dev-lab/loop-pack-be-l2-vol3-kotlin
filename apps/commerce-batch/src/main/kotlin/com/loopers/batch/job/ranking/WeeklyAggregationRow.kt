package com.loopers.batch.job.ranking

/**
 * 주간 집계 Reader 가 내려주는 raw row.
 *
 * Reader 의 `JdbcCursorItemReader` 가 SQL 을 통해 `product_metrics_daily` 7일치를
 * GROUP BY 한 결과 1행을 표현한다. Processor 가 이 데이터를 `WeeklyProductRankModel` 로 변환하며
 * rank_position 은 그 과정에서 부여된다.
 */
data class WeeklyAggregationRow(
    val productId: Long,
    val totalLikes: Long,
    val totalViews: Long,
    val totalSales: Long,
    val score: Double,
)
