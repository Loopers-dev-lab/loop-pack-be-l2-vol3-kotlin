package com.loopers.infrastructure.persistence.jpa

import com.loopers.domain.productmetrics.ProductMetrics
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ProductMetricsJpaRepository : JpaRepository<ProductMetrics, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pm FROM ProductMetrics pm WHERE pm.productId = :productId")
    fun findByProductIdWithLock(productId: Long): ProductMetrics?
}
