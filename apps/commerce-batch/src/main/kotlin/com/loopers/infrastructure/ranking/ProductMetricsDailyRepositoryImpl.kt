package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.ProductMetricsDaily
import com.loopers.domain.ranking.ProductMetricsDailyRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class ProductMetricsDailyRepositoryImpl(
    private val jpa: ProductMetricsDailyJpaRepository,
) : ProductMetricsDailyRepository {

    override fun findDailyOrNull(productId: Long, date: LocalDate): ProductMetricsDaily? =
        jpa.findByIdProductIdAndIdMetricDate(productId, date)

    override fun findAllDailyOn(date: LocalDate): List<ProductMetricsDaily> =
        jpa.findAllByIdMetricDate(date)

    override fun countDailyOn(date: LocalDate): Long =
        jpa.countByIdMetricDate(date)

    override fun save(daily: ProductMetricsDaily): ProductMetricsDaily = jpa.save(daily)
}
