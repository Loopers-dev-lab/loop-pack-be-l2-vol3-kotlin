package com.loopers.infrastructure.persistence

import com.loopers.domain.productmetrics.ProductMetrics
import com.loopers.domain.productmetrics.ProductMetricsRepository
import com.loopers.infrastructure.persistence.jpa.ProductMetricsJpaRepository
import org.springframework.stereotype.Repository

@Repository
class ProductMetricsRepositoryImpl(
    private val jpaRepository: ProductMetricsJpaRepository,
) : ProductMetricsRepository {
    override fun findByProductIdWithLock(productId: Long): ProductMetrics? {
        return jpaRepository.findByProductIdWithLock(productId)
    }

    override fun save(metrics: ProductMetrics): ProductMetrics {
        return jpaRepository.save(metrics)
    }
}
