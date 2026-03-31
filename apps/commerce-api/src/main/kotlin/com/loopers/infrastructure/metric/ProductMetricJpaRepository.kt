package com.loopers.infrastructure.metric

import org.springframework.data.jpa.repository.JpaRepository

interface ProductMetricJpaRepository : JpaRepository<ProductMetricEntity, Long> {
    fun findByProductId(productId: Long): ProductMetricEntity?
    fun findAllByProductIdInAndDeletedAtIsNull(productIds: List<Long>): List<ProductMetricEntity>
}
