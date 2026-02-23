package com.loopers.infrastructure.like

import com.loopers.domain.like.Like
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LikeJpaRepository : JpaRepository<Like, Long> {
    fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean
    fun findByUserIdAndProductId(userId: Long, productId: Long): Like?

    @Query(
        "SELECT l.productId FROM Like l, Product p " +
            "WHERE l.productId = p.id AND l.userId = :userId AND p.deletedAt IS NULL",
    )
    fun findProductIdsByUserId(@Param("userId") userId: Long): List<Long>
}
