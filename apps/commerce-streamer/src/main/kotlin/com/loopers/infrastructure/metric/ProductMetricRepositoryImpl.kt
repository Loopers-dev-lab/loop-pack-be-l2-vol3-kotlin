package com.loopers.infrastructure.metric

import com.loopers.domain.metric.ProductMetric
import com.loopers.domain.metric.ProductMetricRepository
import org.springframework.stereotype.Repository

@Repository
class ProductMetricRepositoryImpl(
    private val productMetricJpaRepository: ProductMetricJpaRepository,
) : ProductMetricRepository {
    override fun findByProductId(productId: Long): ProductMetric? =
        productMetricJpaRepository.findByProductId(productId)?.toDomain()

    override fun save(metric: ProductMetric): ProductMetric {
        val entity = productMetricJpaRepository.findByProductId(metric.productId)
            ?: ProductMetricEntity(productId = metric.productId)
        entity.apply(metric)
        return productMetricJpaRepository.saveAndFlush(entity).toDomain()
    }

    override fun saveAll(metrics: List<ProductMetric>): List<ProductMetric> =
        productMetricJpaRepository.saveAllAndFlush(
            metrics.map { metric ->
                val entity = productMetricJpaRepository.findByProductId(metric.productId)
                    ?: ProductMetricEntity(productId = metric.productId)
                entity.apply(metric)
                entity
            },
        ).map { it.toDomain() }
}
