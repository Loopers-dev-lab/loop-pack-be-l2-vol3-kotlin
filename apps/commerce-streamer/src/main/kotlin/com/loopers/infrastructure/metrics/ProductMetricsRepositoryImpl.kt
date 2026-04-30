package com.loopers.infrastructure.metrics

import com.loopers.domain.metrics.ProductMetrics
import com.loopers.domain.metrics.ProductMetricsRepository
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ProductMetricsRepositoryImpl(
    private val jpaRepository: ProductMetricsJpaRepository,
) : ProductMetricsRepository {

    override fun findByProductIdAndMetricDate(productId: Long, metricDate: LocalDate): ProductMetrics? {
        return jpaRepository.findByProductIdAndMetricDate(productId, metricDate)
    }

    override fun findByProductIdsAndMetricDate(productIds: Set<Long>, metricDate: LocalDate): List<ProductMetrics> {
        if (productIds.isEmpty()) return emptyList()
        return jpaRepository.findByProductIdInAndMetricDate(productIds, metricDate)
    }

    override fun save(metrics: ProductMetrics): ProductMetrics {
        return jpaRepository.save(metrics)
    }
}
