package com.loopers.batch.job.ranking.step

import com.loopers.domain.ranking.MetricsAggregateDto
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.database.JdbcPagingItemReader
import org.springframework.batch.item.database.PagingQueryProvider
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import javax.sql.DataSource

/**
 * product_metrics 기간 합산 Reader.
 *
 * Weekly/Monthly Job 공용. JobParameters의 startDate/endDate를 기반으로
 * 해당 기간의 product_metrics를 상품별로 GROUP BY 합산하여 페이징 조회한다.
 */
@StepScope
@Component
class RankingAggregateReader(
    dataSource: DataSource,
    @Value("#{jobParameters['startDate']}") startDate: String,
    @Value("#{jobParameters['endDate']}") endDate: String,
) : JdbcPagingItemReader<MetricsAggregateDto>() {

    companion object {
        const val CHUNK_SIZE = 1000
    }

    init {
        setName("rankingAggregateReader")
        setDataSource(dataSource)
        setQueryProvider(buildQueryProvider(dataSource))
        setParameterValues(mapOf("startDate" to startDate, "endDate" to endDate))
        setPageSize(CHUNK_SIZE)
        setRowMapper(
            RowMapper { rs, _ ->
                MetricsAggregateDto(
                    productId = rs.getLong("product_id"),
                    likeCount = rs.getLong("like_count"),
                    orderCount = rs.getLong("order_count"),
                    viewCount = rs.getLong("view_count"),
                )
            },
        )
        afterPropertiesSet()
    }

    private fun buildQueryProvider(dataSource: DataSource): PagingQueryProvider {
        val factory = SqlPagingQueryProviderFactoryBean().apply {
            setDataSource(dataSource)
            setSelectClause(
                "product_id, " +
                    "SUM(like_count) as like_count, " +
                    "SUM(order_count) as order_count, " +
                    "SUM(view_count) as view_count",
            )
            setFromClause("from product_metrics")
            setWhereClause("where metric_date BETWEEN :startDate AND :endDate")
            setGroupClause("product_id")
            setSortKey("product_id")
        }
        return factory.getObject()!!
    }
}
