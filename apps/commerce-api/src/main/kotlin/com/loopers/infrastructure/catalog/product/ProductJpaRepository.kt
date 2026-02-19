package com.loopers.infrastructure.catalog.product

import com.loopers.domain.catalog.product.entity.Product
import org.springframework.data.jpa.repository.JpaRepository

interface ProductJpaRepository : JpaRepository<Product, Long> {
    fun findAllByRefBrandId(brandId: Long): List<Product>
}
