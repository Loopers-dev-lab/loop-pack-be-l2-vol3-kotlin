package com.loopers.infrastructure.product

import com.loopers.domain.product.Product
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductJpaRepository : JpaRepository<Product, Long> {

    fun findByIdAndDeletedAtIsNull(id: Long): Product?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
    fun findByIdWithLock(id: Long): Product?

    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<Product>

    fun findAllByBrandIdAndDeletedAtIsNull(brandId: Long, pageable: Pageable): Page<Product>

    fun findAllByBrandIdAndDeletedAtIsNull(brandId: Long): List<Product>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :ids AND p.deletedAt IS NULL")
    fun findAllByIdWithLock(ids: List<Long>): List<Product>

    @Modifying(clearAutomatically = true)
    @Query("UPDATE products SET like_count = like_count + 1 WHERE id = :productId", nativeQuery = true)
    fun incrementLikeCount(@Param("productId") productId: Long)

    @Modifying(clearAutomatically = true)
    @Query(
        "UPDATE products SET like_count = GREATEST(like_count - 1, 0) WHERE id = :productId",
        nativeQuery = true,
    )
    fun decrementLikeCount(@Param("productId") productId: Long)
}
