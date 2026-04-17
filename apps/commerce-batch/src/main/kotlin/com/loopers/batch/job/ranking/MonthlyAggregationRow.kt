package com.loopers.batch.job.ranking

/**
 * 월간 집계 Reader 가 내려주는 raw row.
 *
 * `WeeklyAggregationRow` 와 구조는 동일하지만 도메인(주/월) 분리를 위해 별도 타입으로 유지한다.
 * 실제 추상화는 Phase 6 리팩터링에서 수요가 생길 때 진행한다 (YAGNI).
 */
data class MonthlyAggregationRow(
    val productId: Long,
    val totalLikes: Long,
    val totalViews: Long,
    val totalSales: Long,
    val score: Double,
)
