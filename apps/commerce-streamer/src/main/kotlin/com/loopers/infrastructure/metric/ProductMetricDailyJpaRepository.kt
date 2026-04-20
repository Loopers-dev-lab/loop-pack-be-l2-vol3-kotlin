package com.loopers.infrastructure.metric

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ProductMetricDailyJpaRepository : JpaRepository<ProductMetricDailyEntity, Long> {
    fun findByProductIdAndMetricDate(productId: Long, metricDate: LocalDate): ProductMetricDailyEntity?
}
