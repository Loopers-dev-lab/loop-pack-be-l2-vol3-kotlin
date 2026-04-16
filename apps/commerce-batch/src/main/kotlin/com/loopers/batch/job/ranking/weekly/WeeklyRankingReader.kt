package com.loopers.batch.job.ranking.weekly

import com.loopers.batch.job.ranking.ProductAggregateDto
import com.loopers.batch.job.ranking.productAggregateReader
import com.loopers.domain.ranking.YearWeek
import org.springframework.batch.item.database.JdbcPagingItemReader
import java.time.LocalDate
import javax.sql.DataSource

fun weeklyRankingReader(
    dataSource: DataSource,
    targetDate: LocalDate,
    pageSize: Int,
): JdbcPagingItemReader<ProductAggregateDto> {
    val yearWeek = YearWeek.from(targetDate)
    return productAggregateReader(
        dataSource = dataSource,
        readerName = "weeklyRankingReader",
        startDate = yearWeek.startDate,
        endDate = yearWeek.endDate,
        pageSize = pageSize,
    )
}
