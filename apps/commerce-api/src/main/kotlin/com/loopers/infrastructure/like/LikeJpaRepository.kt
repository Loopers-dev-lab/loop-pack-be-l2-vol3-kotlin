package com.loopers.infrastructure.like

import com.loopers.domain.like.Like
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface LikeJpaRepository : JpaRepository<Like, Long> {
    fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean
    fun findByUserIdAndProductId(userId: Long, productId: Long): Like?
    fun findByUserId(userId: Long): List<Like>

    @Transactional
    @Modifying
    @Query(
        value =
            "INSERT IGNORE INTO likes (user_id, product_id, created_at, updated_at) " +
                "VALUES (:userId, :productId, NOW(), NOW())",
        nativeQuery = true,
    )
    fun insertIgnore(@Param("userId") userId: Long, @Param("productId") productId: Long): Int
}
