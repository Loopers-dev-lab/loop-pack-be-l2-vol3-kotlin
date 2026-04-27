package com.loopers.batch.job.ranking

import org.springframework.batch.item.database.JdbcBatchItemWriter
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import java.time.LocalDateTime
import javax.sql.DataSource

fun rankingStagingWriter(
    dataSource: DataSource,
    stagingTableName: String,
    periodColumnName: String,
): JdbcBatchItemWriter<ProductRankRow> {
    return JdbcBatchItemWriterBuilder<ProductRankRow>()
        .dataSource(dataSource)
        .sql(
            """
            INSERT INTO $stagingTableName ($periodColumnName, product_id, score, rank_num, view_count, like_count, sales_count, updated_at)
            VALUES (:periodKey, :productId, :score, 0, :viewCount, :likeCount, :salesCount, :updatedAt)
            ON DUPLICATE KEY UPDATE
                score = VALUES(score), view_count = VALUES(view_count), like_count = VALUES(like_count),
                sales_count = VALUES(sales_count), updated_at = VALUES(updated_at)
            """.trimIndent(),
        )
        .itemSqlParameterSourceProvider { item ->
            MapSqlParameterSource()
                .addValue("periodKey", item.periodKey)
                .addValue("productId", item.productId)
                .addValue("score", item.score)
                .addValue("viewCount", item.viewCount)
                .addValue("likeCount", item.likeCount)
                .addValue("salesCount", item.salesCount)
                .addValue("updatedAt", LocalDateTime.now())
        }
        .build()
}
