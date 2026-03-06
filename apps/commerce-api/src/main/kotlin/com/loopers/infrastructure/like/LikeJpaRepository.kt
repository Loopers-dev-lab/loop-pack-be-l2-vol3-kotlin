package com.loopers.infrastructure.like

import com.loopers.domain.like.LikeModel
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface LikeJpaRepository : JpaRepository<LikeModel, Long> {
    fun findByUserIdAndProductId(userId: Long, productId: Long): LikeModel?
    fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean
    fun findAllByUserId(userId: Long, pageable: Pageable): List<LikeModel>

    @Modifying
    @Query("DELETE FROM LikeModel l WHERE l.userId = :userId AND l.productId = :productId")
    fun deleteByUserIdAndProductId(userId: Long, productId: Long): Int
}
