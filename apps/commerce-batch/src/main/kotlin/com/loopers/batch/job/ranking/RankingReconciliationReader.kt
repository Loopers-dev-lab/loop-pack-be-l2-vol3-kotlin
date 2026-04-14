package com.loopers.batch.job.ranking

import org.springframework.batch.item.database.JdbcPagingItemReader
import org.springframework.batch.item.database.Order
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder
import org.springframework.jdbc.core.RowMapper
import javax.sql.DataSource

fun rankingReconciliationReader(dataSource: DataSource, pageSize: Int): JdbcPagingItemReader<RankingScore> {
    return JdbcPagingItemReaderBuilder<RankingScore>()
        .name("rankingReconciliationReader")
        .dataSource(dataSource)
        .selectClause("SELECT pm.product_id, pm.view_count, pm.like_count, pm.sales_count")
        .fromClause("FROM product_metrics pm INNER JOIN products p ON pm.product_id = p.id")
        .whereClause("WHERE p.deleted_at IS NULL")
        .sortKeys(mapOf("pm.product_id" to Order.ASCENDING))
        .rowMapper(
            RowMapper { rs, _ ->
                RankingScore(
                    productId = rs.getLong("product_id"),
                    viewCount = rs.getLong("view_count"),
                    likeCount = rs.getLong("like_count"),
                    salesCount = rs.getLong("sales_count"),
                )
            },
        )
        .pageSize(pageSize)
        .build()
}
