package com.loopers.infrastructure.product

import com.loopers.domain.product.Product
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.querydsl.QuerydslPredicateExecutor
import org.springframework.stereotype.Repository

@Repository
interface ProductJpaRepository : JpaRepository<Product, Long>, QuerydslPredicateExecutor<Product> {
    fun findByBrandId(brandId: Long): List<Product>


    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        "UPDATE products SET like_count = like_count + 1 WHERE id = :productId",
        nativeQuery = true,
    )
    fun incrementLikeCountAtomic(productId: Long)

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        "UPDATE products SET like_count = GREATEST(like_count - 1, 0) WHERE id = :productId",
        nativeQuery = true,
    )
    fun decrementLikeCountAtomic(productId: Long)
}
