package com.loopers.batch.job.ranking

import java.time.format.DateTimeFormatter

object RankingBatchConstants {
    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    const val CHUNK_SIZE = 100
    const val RANKING_LIMIT = 100

    // bucket_date >= :from AND bucket_date < :to 로 기간 윈도우 집계.
    // 같은 product_id가 여러 일자에 흩어져 있으므로 SUM 후 score 계산.
    // tie-breaker product_id ASC 로 동점 시에도 결정적 순서 보장.
    val PRODUCT_METRICS_RANKING_SQL =
        """
        SELECT product_id,
               (SUM(view_count) * 0.1 + SUM(like_count) * 0.2 + SUM(order_count) * 0.7) AS score
        FROM product_metrics
        WHERE bucket_date >= ? AND bucket_date < ?
        GROUP BY product_id
        ORDER BY score DESC, product_id ASC
        LIMIT $RANKING_LIMIT
        """.trimIndent()
}
