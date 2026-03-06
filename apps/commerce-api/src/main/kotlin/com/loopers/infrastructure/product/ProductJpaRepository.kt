package com.loopers.infrastructure.product

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductJpaRepository : JpaRepository<ProductEntity, Long> {
    fun findAllByBrandId(brandId: Long): List<ProductEntity>
    fun findAllByIdIn(ids: List<Long>): List<ProductEntity>
    fun existsByBrandIdAndStatus(brandId: Long, status: String): Boolean

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProductEntity p SET p.stock = p.stock - :quantity WHERE p.id = :id AND p.stock >= :quantity")
    fun deductStock(@Param("id") id: Long, @Param("quantity") quantity: Int): Int

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProductEntity p SET p.stock = p.stock + :quantity WHERE p.id = :id")
    fun restoreStock(@Param("id") id: Long, @Param("quantity") quantity: Int): Int
}
