package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.ProductMetricsDaily
import com.loopers.domain.ranking.ProductMetricsDailyId
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ProductMetricsDailyJpaRepository : JpaRepository<ProductMetricsDaily, ProductMetricsDailyId> {

    fun findByIdProductIdAndIdMetricDate(productId: Long, metricDate: LocalDate): ProductMetricsDaily?

    fun findAllByIdMetricDate(metricDate: LocalDate): List<ProductMetricsDaily>

    fun countByIdMetricDate(metricDate: LocalDate): Long
}
