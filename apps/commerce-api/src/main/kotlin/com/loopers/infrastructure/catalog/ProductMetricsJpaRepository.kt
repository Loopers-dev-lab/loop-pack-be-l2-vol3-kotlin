package com.loopers.infrastructure.catalog

import com.loopers.domain.catalog.ProductMetricsModel
import org.springframework.data.jpa.repository.JpaRepository

interface ProductMetricsJpaRepository : JpaRepository<ProductMetricsModel, Long> {
    fun findByProductId(productId: Long): ProductMetricsModel?
}
