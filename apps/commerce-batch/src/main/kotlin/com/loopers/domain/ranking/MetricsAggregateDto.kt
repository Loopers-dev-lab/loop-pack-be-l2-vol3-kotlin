package com.loopers.domain.ranking

/**
 * product_metrics 기간 합산 결과 DTO.
 *
 * JdbcPagingItemReader가 GROUP BY 결과를 매핑하는 데 사용한다.
 */
data class MetricsAggregateDto(
    val productId: Long = 0,
    val likeCount: Long = 0,
    val orderCount: Long = 0,
    val viewCount: Long = 0,
)
