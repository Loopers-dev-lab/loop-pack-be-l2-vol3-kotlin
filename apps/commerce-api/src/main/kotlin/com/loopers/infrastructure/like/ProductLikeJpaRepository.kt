package com.loopers.infrastructure.like

import com.loopers.domain.like.ProductLikeModel
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductLikeJpaRepository : JpaRepository<ProductLikeModel, Long> {
    fun findByUserIdAndProductId(userId: Long, productId: Long): ProductLikeModel?
    fun findAllByUserId(userId: Long, pageable: Pageable): Slice<ProductLikeModel>

    @Modifying
    @Query(
        value = "INSERT IGNORE INTO product_likes (user_id, product_id, created_at, updated_at) " +
            "VALUES (:userId, :productId, NOW(), NOW())",
        nativeQuery = true,
    )
    fun insertIgnore(@Param("userId") userId: Long, @Param("productId") productId: Long): Int
}
