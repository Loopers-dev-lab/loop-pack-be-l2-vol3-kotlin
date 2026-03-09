package com.loopers.infrastructure.like

import com.loopers.domain.like.ProductLikeModel
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository

interface ProductLikeJpaRepository : JpaRepository<ProductLikeModel, Long> {
    fun findByUserIdAndProductId(userId: Long, productId: Long): ProductLikeModel?
    fun findAllByUserId(userId: Long, pageable: Pageable): Slice<ProductLikeModel>
}
