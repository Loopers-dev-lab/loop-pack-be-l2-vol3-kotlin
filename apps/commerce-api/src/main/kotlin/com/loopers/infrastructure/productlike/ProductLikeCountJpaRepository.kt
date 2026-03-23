package com.loopers.infrastructure.productlike

import com.loopers.domain.productlike.ProductLikeCount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ProductLikeCountJpaRepository : JpaRepository<ProductLikeCount, Long> {
    fun findByProductId(productId: Long): ProductLikeCount?

    @Modifying(flushAutomatically = true)
    @Query(
        value = "INSERT INTO product_like_counts (product_id, like_count, created_at, updated_at, deleted_at) " +
            "VALUES (:productId, 1, NOW(6), NOW(6), NULL) " +
            "ON DUPLICATE KEY UPDATE like_count = like_count + 1, updated_at = NOW(6)",
        nativeQuery = true,
    )
    fun increment(productId: Long)

    @Modifying(flushAutomatically = true)
    @Query(
        value = "UPDATE product_like_counts SET like_count = GREATEST(like_count - 1, 0), updated_at = NOW(6) " +
            "WHERE product_id = :productId",
        nativeQuery = true,
    )
    fun decrement(productId: Long)

    @Modifying(flushAutomatically = true)
    @Query(
        value = "UPDATE product_like_counts SET like_count = :count, updated_at = NOW(6) " +
            "WHERE product_id = :productId",
        nativeQuery = true,
    )
    fun updateCount(productId: Long, count: Long)
}
