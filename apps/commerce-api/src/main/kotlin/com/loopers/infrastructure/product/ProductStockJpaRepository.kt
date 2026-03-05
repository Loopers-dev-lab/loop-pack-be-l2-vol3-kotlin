package com.loopers.infrastructure.product

import org.springframework.data.jpa.repository.JpaRepository

interface ProductStockJpaRepository : JpaRepository<ProductStockEntity, Long> {
    fun findByProductIdAndDeletedAtIsNull(productId: Long): ProductStockEntity?
    fun findAllByProductIdInAndDeletedAtIsNull(productIds: List<Long>): List<ProductStockEntity>
}
