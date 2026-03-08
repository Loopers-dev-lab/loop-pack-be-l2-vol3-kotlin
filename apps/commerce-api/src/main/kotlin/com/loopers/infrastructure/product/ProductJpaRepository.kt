package com.loopers.infrastructure.product

import com.loopers.domain.product.Product
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ProductJpaRepository : JpaRepository<Product, Long> {
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<Product>
    fun findAllByBrandIdAndDeletedAtIsNull(brandId: Long, pageable: Pageable): Page<Product>
    fun findByIdAndDeletedAtIsNull(id: Long): Product?
    fun findAllByIdInAndDeletedAtIsNull(ids: List<Long>): List<Product>
    fun findAllByBrandIdAndDeletedAtIsNull(brandId: Long): List<Product>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
    fun findByIdWithLock(id: Long): Product?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :ids AND p.deletedAt IS NULL ORDER BY p.id")
    fun findAllByIdInWithLock(ids: List<Long>): List<Product>

    @Modifying(clearAutomatically = true)
    @Query("UPDATE products SET likes = likes + 1 WHERE id = :productId", nativeQuery = true)
    fun incrementLikeCount(productId: Long)

    @Modifying(clearAutomatically = true)
    @Query("UPDATE products SET likes = CASE WHEN likes > 0 THEN likes - 1 ELSE 0 END WHERE id = :productId", nativeQuery = true)
    fun decrementLikeCount(productId: Long)
}
