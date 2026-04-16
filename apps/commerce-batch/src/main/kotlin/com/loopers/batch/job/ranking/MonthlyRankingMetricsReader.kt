package com.loopers.batch.job.ranking

import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemStream
import org.springframework.batch.item.database.JdbcPagingItemReader
import org.springframework.batch.item.database.Order
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.sql.Date
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@StepScope
@Component
class MonthlyRankingMetricsReader(
    private val dataSource: DataSource,
) : ItemReader<ProductMetricsDailyRow>, ItemStream {

    @Value("#{jobParameters['targetDate']}")
    private lateinit var targetDate: String

    private lateinit var delegate: JdbcPagingItemReader<ProductMetricsDailyRow>

    override fun open(executionContext: ExecutionContext) {
        val date = LocalDate.parse(targetDate, DateTimeFormatter.BASIC_ISO_DATE)
        val (firstDay, lastDay) = getMonthRange(date)

        val queryProvider = MySqlPagingQueryProvider().apply {
            setSelectClause("SELECT product_id, view_count, sales_count, like_count")
            setFromClause("FROM product_metrics_daily")
            setWhereClause("WHERE metric_date BETWEEN :startDate AND :endDate")
            setSortKeys(sortedMapOf("product_id" to Order.ASCENDING, "metric_date" to Order.ASCENDING))
        }

        delegate = JdbcPagingItemReader<ProductMetricsDailyRow>().apply {
            setDataSource(dataSource)
            setQueryProvider(queryProvider)
            setParameterValues(
                mapOf(
                    "startDate" to Date.valueOf(firstDay),
                    "endDate" to Date.valueOf(lastDay),
                ),
            )
            setRowMapper { rs, _ ->
                ProductMetricsDailyRow(
                    productId = rs.getLong("product_id"),
                    viewCount = rs.getLong("view_count"),
                    salesCount = rs.getLong("sales_count"),
                    likeCount = rs.getLong("like_count"),
                )
            }
            setPageSize(500)
            afterPropertiesSet()
        }
        delegate.open(executionContext)
    }

    override fun read(): ProductMetricsDailyRow? = delegate.read()

    override fun update(executionContext: ExecutionContext) = delegate.update(executionContext)

    override fun close() = delegate.close()

    companion object {
        fun getMonthRange(date: LocalDate): Pair<LocalDate, LocalDate> {
            val firstDay = date.withDayOfMonth(1)
            val lastDay = date.withDayOfMonth(date.lengthOfMonth())
            return firstDay to lastDay
        }
    }
}
