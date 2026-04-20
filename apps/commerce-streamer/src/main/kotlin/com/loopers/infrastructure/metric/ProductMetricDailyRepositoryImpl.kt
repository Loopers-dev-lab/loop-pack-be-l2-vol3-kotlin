package com.loopers.infrastructure.metric

import com.loopers.domain.metric.ProductMetricDaily
import com.loopers.domain.metric.ProductMetricDailyRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class ProductMetricDailyRepositoryImpl(
    private val productMetricDailyJpaRepository: ProductMetricDailyJpaRepository,
) : ProductMetricDailyRepository {

    override fun findByProductIdAndMetricDate(productId: Long, metricDate: LocalDate): ProductMetricDaily? =
        productMetricDailyJpaRepository.findByProductIdAndMetricDate(productId, metricDate)?.toDomain()

    override fun save(metric: ProductMetricDaily): ProductMetricDaily {
        val entity = productMetricDailyJpaRepository.findByProductIdAndMetricDate(metric.productId, metric.metricDate)
            ?: ProductMetricDailyEntity(productId = metric.productId, metricDate = metric.metricDate)
        entity.apply(metric)
        return productMetricDailyJpaRepository.saveAndFlush(entity).toDomain()
    }
}
