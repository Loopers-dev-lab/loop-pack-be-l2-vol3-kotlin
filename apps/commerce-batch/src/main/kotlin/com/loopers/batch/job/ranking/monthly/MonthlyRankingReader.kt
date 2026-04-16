package com.loopers.batch.job.ranking.monthly

import com.loopers.batch.job.ranking.ProductAggregateDto
import org.springframework.batch.item.database.JdbcPagingItemReader
import org.springframework.batch.item.database.Order
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder
import org.springframework.jdbc.core.RowMapper
import java.time.LocalDate
import java.time.YearMonth
import javax.sql.DataSource

fun monthlyRankingReader(
    dataSource: DataSource,
    targetDate: LocalDate,
    pageSize: Int,
): JdbcPagingItemReader<ProductAggregateDto> {
    val yearMonth = YearMonth.from(targetDate)
    val startDate = yearMonth.atDay(1)
    val endDate = yearMonth.atEndOfMonth()

    return JdbcPagingItemReaderBuilder<ProductAggregateDto>()
        .name("monthlyRankingReader")
        .dataSource(dataSource)
        .selectClause(
            "SELECT product_id, SUM(view_count) AS view_count, SUM(like_count) AS like_count, SUM(sales_count) AS sales_count",
        )
        .fromClause("FROM product_metrics_daily")
        .whereClause("WHERE metric_date BETWEEN :startDate AND :endDate")
        .groupClause("GROUP BY product_id")
        .sortKeys(mapOf("product_id" to Order.ASCENDING))
        .parameterValues(
            mapOf(
                "startDate" to startDate,
                "endDate" to endDate,
            ),
        )
        .rowMapper(
            RowMapper { rs, _ ->
                ProductAggregateDto(
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
