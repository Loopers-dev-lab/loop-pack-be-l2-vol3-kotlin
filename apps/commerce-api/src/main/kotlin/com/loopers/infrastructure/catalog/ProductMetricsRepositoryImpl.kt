package com.loopers.infrastructure.catalog

import com.loopers.domain.catalog.ProductMetricsModel
import com.loopers.domain.catalog.ProductMetricsRepository
import org.springframework.stereotype.Component

@Component
class ProductMetricsRepositoryImpl(
    private val productMetricsJpaRepository: ProductMetricsJpaRepository,
) : ProductMetricsRepository {

    override fun findByProductId(productId: Long): ProductMetricsModel? {
        return productMetricsJpaRepository.findByProductId(productId)
    }

    override fun save(metrics: ProductMetricsModel): ProductMetricsModel {
        return productMetricsJpaRepository.save(metrics)
    }

    override fun saveAll(metrics: List<ProductMetricsModel>): List<ProductMetricsModel> {
        return productMetricsJpaRepository.saveAll(metrics)
    }
}
