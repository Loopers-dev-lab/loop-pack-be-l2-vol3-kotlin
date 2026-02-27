package com.loopers.infrastructure.productlike

import com.loopers.domain.productlike.ProductLike
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ProductLikeJpaRepository : JpaRepository<ProductLike, Long> {
    @Query("SELECT pl FROM ProductLike pl WHERE pl.user.id = :userId AND pl.product.id = :productId")
    fun findByUserIdAndProductId(userId: Long, productId: Long): ProductLike?
}
