package com.loopers.infrastructure.metrics

import com.loopers.domain.metrics.model.ProductMetrics
import com.loopers.domain.metrics.repository.ProductMetricsRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface ProductMetricsJpaRepository : JpaRepository<ProductMetricsEntity, Long> {
    fun findByProductId(productId: Long): ProductMetricsEntity?
}

@Repository
class ProductMetricsRepositoryImpl(
    private val productMetricsJpaRepository: ProductMetricsJpaRepository,
) : ProductMetricsRepository {

    override fun findByProductId(productId: Long): ProductMetrics? {
        return productMetricsJpaRepository.findByProductId(productId)?.toDomain()
    }

    override fun save(metrics: ProductMetrics): ProductMetrics {
        return productMetricsJpaRepository.save(ProductMetricsEntity.fromDomain(metrics)).toDomain()
    }
}
