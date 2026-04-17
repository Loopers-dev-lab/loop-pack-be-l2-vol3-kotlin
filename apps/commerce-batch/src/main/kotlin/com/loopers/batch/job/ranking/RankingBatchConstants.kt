package com.loopers.batch.job.ranking

import java.time.format.DateTimeFormatter

object RankingBatchConstants {
    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    const val CHUNK_SIZE = 100
    const val RANKING_LIMIT = 100

    val PRODUCT_METRICS_RANKING_SQL =
        """
        SELECT product_id,
               (view_count * 0.1 + like_count * 0.2 + order_count * 0.7) AS score
        FROM product_metrics
        ORDER BY score DESC
        LIMIT $RANKING_LIMIT
        """.trimIndent()
}
