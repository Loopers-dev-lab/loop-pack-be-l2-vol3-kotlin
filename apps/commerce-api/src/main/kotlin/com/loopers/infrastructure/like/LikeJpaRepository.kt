package com.loopers.infrastructure.like

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface LikeJpaRepository : JpaRepository<LikeEntity, Long> {

    @Modifying
    @Query(
        value = "INSERT IGNORE INTO likes (user_id, product_id, created_at) VALUES (:userId, :productId, NOW())",
        nativeQuery = true,
    )
    fun insertIgnore(userId: Long, productId: Long): Int

    @Modifying
    @Query("DELETE FROM LikeEntity l WHERE l.userId = :userId AND l.productId = :productId")
    fun deleteByUserIdAndProductId(userId: Long, productId: Long): Int

    @Query("SELECT l FROM LikeEntity l WHERE l.userId = :userId ORDER BY l.createdAt DESC")
    fun findAllByUserId(userId: Long): List<LikeEntity>
}
