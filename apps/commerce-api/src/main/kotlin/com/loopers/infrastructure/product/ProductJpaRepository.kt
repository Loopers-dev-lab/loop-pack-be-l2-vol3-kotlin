package com.loopers.infrastructure.product

import com.loopers.domain.product.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductJpaRepository : JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Query("SELECT p FROM Product p WHERE p.id = :id")
    fun findByIdOrNull(@Param("id") id: Long): Product?

    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
    fun findActiveByIdOrNull(@Param("id") id: Long): Product?

    @Query("SELECT p FROM Product p WHERE p.brandId = :brandId AND p.deletedAt IS NULL")
    fun findAllActiveByBrandId(@Param("brandId") brandId: Long): List<Product>

    @Query("SELECT p FROM Product p WHERE p.id IN :ids AND p.deletedAt IS NULL")
    fun findAllActiveByIds(@Param("ids") ids: List<Long>): List<Product>

    @Query("SELECT p FROM Product p WHERE p.id IN :ids")
    fun findAllByIds(@Param("ids") ids: List<Long>): List<Product>

    @Modifying
    @Query("UPDATE Product p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    fun increaseLikeCount(@Param("id") id: Long)

    @Modifying
    @Query("UPDATE Product p SET p.likeCount = CASE WHEN p.likeCount > 0 THEN p.likeCount - 1 ELSE 0 END WHERE p.id = :id")
    fun decreaseLikeCount(@Param("id") id: Long)
}
