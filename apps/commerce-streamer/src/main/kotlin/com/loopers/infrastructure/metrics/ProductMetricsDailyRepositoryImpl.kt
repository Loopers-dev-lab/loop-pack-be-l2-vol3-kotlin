package com.loopers.infrastructure.metrics

import com.loopers.domain.metrics.model.ProductMetricsDaily
import com.loopers.domain.metrics.repository.ProductMetricsDailyRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

interface ProductMetricsDailyJpaRepository : JpaRepository<ProductMetricsDailyEntity, Long> {
    fun findByMetricDateAndProductId(metricDate: LocalDate, productId: Long): ProductMetricsDailyEntity?
}

@Repository
class ProductMetricsDailyRepositoryImpl(
    private val productMetricsDailyJpaRepository: ProductMetricsDailyJpaRepository,
) : ProductMetricsDailyRepository {

    override fun findByDateAndProductId(metricDate: LocalDate, productId: Long): ProductMetricsDaily? {
        return productMetricsDailyJpaRepository.findByMetricDateAndProductId(metricDate, productId)?.toDomain()
    }

    override fun save(daily: ProductMetricsDaily): ProductMetricsDaily {
        return productMetricsDailyJpaRepository.save(ProductMetricsDailyEntity.fromDomain(daily)).toDomain()
    }
}
