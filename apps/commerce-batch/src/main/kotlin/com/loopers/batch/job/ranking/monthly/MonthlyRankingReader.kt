package com.loopers.batch.job.ranking.monthly

import com.loopers.batch.job.ranking.ProductAggregateDto
import com.loopers.batch.job.ranking.productAggregateReader
import org.springframework.batch.item.database.JdbcPagingItemReader
import java.time.LocalDate
import java.time.YearMonth
import javax.sql.DataSource

fun monthlyRankingReader(
    dataSource: DataSource,
    targetDate: LocalDate,
    pageSize: Int,
): JdbcPagingItemReader<ProductAggregateDto> {
    val yearMonth = YearMonth.from(targetDate)
    return productAggregateReader(
        dataSource = dataSource,
        readerName = "monthlyRankingReader",
        startDate = yearMonth.atDay(1),
        endDate = yearMonth.atEndOfMonth(),
        pageSize = pageSize,
    )
}
