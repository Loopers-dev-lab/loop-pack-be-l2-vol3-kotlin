package com.loopers.infrastructure.product

import com.loopers.domain.product.ProductInventoryModel
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductInventoryJpaRepository : JpaRepository<ProductInventoryModel, Long> {
    fun findByProductId(productId: Long): ProductInventoryModel?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductInventoryModel p WHERE p.productId = :productId")
    fun findByProductIdForUpdate(@Param("productId") productId: Long): ProductInventoryModel?
}
