package com.loopers.batch.job.ranking.step

import com.loopers.domain.ranking.ProductRankAggregation
import org.springframework.batch.item.database.JdbcCursorItemReader
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder
import java.time.LocalDate
import javax.sql.DataSource

object RankingAggregationReader {

    fun create(
        name: String,
        dataSource: DataSource,
        startDate: LocalDate,
        endDate: LocalDate,
        weights: ScoreWeights,
        topN: Int,
        fetchSize: Int,
    ): JdbcCursorItemReader<ProductRankAggregation> =
        JdbcCursorItemReaderBuilder<ProductRankAggregation>()
            .name(name)
            .dataSource(dataSource)
            .sql(buildAggregationSql(topN))
            .preparedStatementSetter { ps ->
                ps.setDouble(1, weights.viewWeight)
                ps.setDouble(2, weights.likeWeight)
                ps.setDouble(3, weights.orderWeight)
                ps.setObject(4, startDate)
                ps.setObject(5, endDate)
            }
            .rowMapper { rs, _ ->
                ProductRankAggregation(
                    productId = rs.getLong("product_id"),
                    totalScore = rs.getDouble("total_score"),
                    viewCount = rs.getInt("view_count"),
                    likeCount = rs.getInt("like_count"),
                    orderCount = rs.getInt("order_count"),
                )
            }
            .fetchSize(fetchSize)
            .build()

    private fun buildAggregationSql(topN: Int): String =
        """
        SELECT
            rel.product_id,
            SUM(CASE WHEN rel.event_type = 'VIEW'  THEN rel.event_value * ? ELSE 0 END) +
            SUM(CASE WHEN rel.event_type = 'LIKE'  THEN rel.event_value * ? ELSE 0 END) +
            SUM(CASE WHEN rel.event_type = 'ORDER' THEN rel.event_value * ? ELSE 0 END) AS total_score,
            COUNT(CASE WHEN rel.event_type = 'VIEW'  THEN 1 END) AS view_count,
            COUNT(CASE WHEN rel.event_type = 'LIKE'  THEN 1 END) AS like_count,
            COUNT(CASE WHEN rel.event_type = 'ORDER' THEN 1 END) AS order_count
        FROM ranking_event_log rel
        WHERE rel.occurred_date BETWEEN ? AND ?
        GROUP BY rel.product_id
        ORDER BY total_score DESC
        LIMIT $topN
        """.trimIndent()

    data class ScoreWeights(
        val viewWeight: Double,
        val likeWeight: Double,
        val orderWeight: Double,
    )
}
