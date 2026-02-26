package com.loopers.infrastructure.product

import com.loopers.domain.product.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductJpaRepository : JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Query("SELECT p FROM Product p WHERE p.id = :id")
    fun findByIdOrNull(@Param("id") id: Long): Product?

    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
    fun findActiveByIdOrNull(@Param("id") id: Long): Product?

    @Query("SELECT p FROM Product p WHERE p.brandId = :brandId AND p.deletedAt IS NULL")
    fun findAllActiveByBrandId(@Param("brandId") brandId: Long): List<Product>
}
