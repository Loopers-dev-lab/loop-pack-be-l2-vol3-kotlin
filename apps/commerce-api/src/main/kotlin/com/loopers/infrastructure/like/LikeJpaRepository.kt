package com.loopers.infrastructure.like

import com.loopers.domain.like.Like
import com.loopers.domain.like.LikeRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LikeJpaRepository : JpaRepository<Like, Long>, LikeRepository {

    @Query("SELECT l FROM Like l WHERE l.userId = :userId AND l.productId = :productId")
    override fun findByUserIdAndProductId(
        @Param("userId") userId: Long,
        @Param("productId") productId: Long,
    ): Like?

    @Query(
        "SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END " +
            "FROM Like l WHERE l.userId = :userId AND l.productId = :productId",
    )
    override fun existsByUserIdAndProductId(
        @Param("userId") userId: Long,
        @Param("productId") productId: Long,
    ): Boolean

    @Query("SELECT l FROM Like l WHERE l.userId = :userId ORDER BY l.createdAt DESC")
    override fun findAllByUserId(@Param("userId") userId: Long): List<Like>
}
