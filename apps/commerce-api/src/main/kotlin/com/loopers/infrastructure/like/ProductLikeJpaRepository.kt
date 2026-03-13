package com.loopers.infrastructure.like

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ProductLikeJpaRepository : JpaRepository<ProductLikeEntity, Long> {
    fun existsByUserIdAndProductId(
        userId: Long,
        productId: Long,
    ): Boolean

    fun findAllByUserId(
        userId: Long,
        pageable: Pageable,
    ): Page<ProductLikeEntity>

    fun countByProductId(productId: Long): Long
}
