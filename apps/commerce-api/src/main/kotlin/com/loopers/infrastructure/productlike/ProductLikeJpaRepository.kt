package com.loopers.infrastructure.productlike

import com.loopers.domain.productlike.ProductLike
import org.springframework.data.jpa.repository.JpaRepository

interface ProductLikeJpaRepository : JpaRepository<ProductLike, Long> {
    fun findByUserIdAndProductId(userId: Long, productId: Long): ProductLike?
}
