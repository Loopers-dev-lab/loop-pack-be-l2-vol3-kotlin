package com.loopers.infrastructure.like

import com.loopers.domain.like.Like
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LikeJpaRepository : JpaRepository<Like, Long> {

    @Query("SELECT l FROM Like l WHERE l.userId = :userId AND l.productId = :productId")
    fun findByUserIdAndProductId(
        @Param("userId") userId: Long,
        @Param("productId") productId: Long,
    ): Like?

    @Query(
        "SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END " +
            "FROM Like l WHERE l.userId = :userId AND l.productId = :productId",
    )
    fun existsByUserIdAndProductId(
        @Param("userId") userId: Long,
        @Param("productId") productId: Long,
    ): Boolean

    @Query(
        "SELECT l FROM Like l WHERE l.userId = :userId " +
            "AND l.productId IN (SELECT p.id FROM Product p WHERE p.deletedAt IS NULL) " +
            "ORDER BY l.createdAt DESC",
        countQuery = "SELECT COUNT(l) FROM Like l WHERE l.userId = :userId " +
            "AND l.productId IN (SELECT p.id FROM Product p WHERE p.deletedAt IS NULL)",
    )
    fun findActiveLikesByUserId(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): Page<Like>
}
