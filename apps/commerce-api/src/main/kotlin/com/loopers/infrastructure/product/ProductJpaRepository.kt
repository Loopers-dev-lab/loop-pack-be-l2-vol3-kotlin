package com.loopers.infrastructure.product

import com.loopers.domain.product.Product
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ProductJpaRepository : JpaRepository<Product, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): Product?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
    fun findByIdWithPessimisticLock(id: Long): Product?

    @Modifying
    @Query("UPDATE Product p SET p.likeCount = p.likeCount + 1 WHERE p.id = :productId")
    fun increaseLikeCount(productId: Long)

    @Modifying
    @Query("UPDATE Product p SET p.likeCount = p.likeCount - 1 WHERE p.id = :productId AND p.likeCount > 0")
    fun decreaseLikeCount(productId: Long)

    @Modifying
    @Query("UPDATE Product p SET p.deletedAt = CURRENT_TIMESTAMP WHERE p.brandId = :brandId AND p.deletedAt IS NULL")
    fun softDeleteByBrandId(brandId: Long)
}
