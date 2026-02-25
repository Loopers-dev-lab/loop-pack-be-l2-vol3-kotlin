package com.loopers.infrastructure.product

import org.springframework.data.jpa.repository.JpaRepository

interface ProductJpaRepository : JpaRepository<ProductEntity, Long> {
    fun findAllByBrandId(brandId: Long): List<ProductEntity>
    fun findAllByIdIn(ids: List<Long>): List<ProductEntity>
    fun existsByBrandIdAndStatus(brandId: Long, status: String): Boolean
}
