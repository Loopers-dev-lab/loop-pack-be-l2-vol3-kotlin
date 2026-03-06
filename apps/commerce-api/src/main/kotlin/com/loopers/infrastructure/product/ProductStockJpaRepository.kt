package com.loopers.infrastructure.product

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface ProductStockJpaRepository : JpaRepository<ProductStockEntity, Long> {
    fun findByProductIdAndDeletedAtIsNull(productId: Long): ProductStockEntity?
    fun findAllByProductIdInAndDeletedAtIsNull(productIds: List<Long>): List<ProductStockEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "SELECT ps FROM ProductStockEntity ps " +
            "WHERE ps.productId IN :productIds AND ps.deletedAt IS NULL " +
            "ORDER BY ps.productId ASC",
    )
    fun findAllByProductIdInWithLock(productIds: List<Long>): List<ProductStockEntity>
}
