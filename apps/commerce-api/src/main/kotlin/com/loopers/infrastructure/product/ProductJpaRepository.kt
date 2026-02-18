package com.loopers.infrastructure.product

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ProductJpaRepository : JpaRepository<ProductEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductEntity p LEFT JOIN FETCH p.images WHERE p.id = :id")
    fun findByIdForUpdate(id: Long): ProductEntity?

    @Query("SELECT p FROM ProductEntity p LEFT JOIN FETCH p.images WHERE p.id = :id")
    fun findByIdWithImages(id: Long): ProductEntity?

    @Modifying
    @Query("UPDATE ProductEntity p SET p.stock = p.stock - :quantity WHERE p.id = :id AND p.stock >= :quantity")
    fun decreaseStock(id: Long, quantity: Int): Int

    @Modifying
    @Query("UPDATE ProductEntity p SET p.stock = p.stock + :quantity WHERE p.id = :id")
    fun increaseStock(id: Long, quantity: Int): Int

    @Modifying
    @Query("UPDATE ProductEntity p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    fun incrementLikeCount(id: Long): Int

    @Modifying
    @Query("UPDATE ProductEntity p SET p.likeCount = p.likeCount - 1 WHERE p.id = :id AND p.likeCount > 0")
    fun decrementLikeCount(id: Long): Int

    @Modifying
    @Query("UPDATE ProductEntity p SET p.deletedAt = CURRENT_TIMESTAMP WHERE p.brandId = :brandId AND p.deletedAt IS NULL")
    fun softDeleteByBrandId(brandId: Long): Int

    @Query("SELECT p FROM ProductEntity p WHERE p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    fun findAllActiveLatest(): List<ProductEntity>

    @Query("SELECT p FROM ProductEntity p WHERE p.deletedAt IS NULL ORDER BY p.likeCount DESC")
    fun findAllActivePopular(): List<ProductEntity>

    @Query("SELECT p FROM ProductEntity p WHERE p.brandId = :brandId AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    fun findAllActiveByBrandLatest(brandId: Long): List<ProductEntity>

    @Query("SELECT p FROM ProductEntity p WHERE p.brandId = :brandId AND p.deletedAt IS NULL ORDER BY p.likeCount DESC")
    fun findAllActiveByBrandPopular(brandId: Long): List<ProductEntity>

    @Query("SELECT p FROM ProductEntity p WHERE p.brandId = :brandId ORDER BY p.createdAt DESC")
    fun findAllByBrand(brandId: Long): List<ProductEntity>

    @Query("SELECT p FROM ProductEntity p ORDER BY p.createdAt DESC")
    fun findAllLatest(): List<ProductEntity>
}
