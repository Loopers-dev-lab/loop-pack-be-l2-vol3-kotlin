package com.loopers.infrastructure.like

import com.loopers.domain.like.ProductLike
import org.springframework.data.jpa.repository.JpaRepository

interface ProductLikeJpaRepository : JpaRepository<ProductLike, Long> {
    fun findByUserIdAndProductId(userId: Long, productId: Long): ProductLike?
    fun findAllByUserId(userId: Long): List<ProductLike>
}
