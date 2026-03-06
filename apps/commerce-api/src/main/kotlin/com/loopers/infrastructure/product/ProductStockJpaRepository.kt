package com.loopers.infrastructure.product

import com.loopers.domain.product.ProductStock
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductStockJpaRepository : JpaRepository<ProductStock, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ps FROM ProductStock ps WHERE ps.productId = :productId")
    fun findByProductIdForUpdate(@Param("productId") productId: Long): ProductStock?

    @Query("SELECT ps FROM ProductStock ps WHERE ps.productId = :productId")
    fun findByProductId(@Param("productId") productId: Long): ProductStock?
}
