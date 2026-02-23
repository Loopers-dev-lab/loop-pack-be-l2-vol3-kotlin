package com.loopers.infrastructure.product

import com.loopers.domain.product.Name
import com.loopers.domain.product.ProductModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ProductJpaRepository : JpaRepository<ProductModel, Long> {
    fun findAllByBrandId(brandId: Long, pageable: Pageable): Page<ProductModel>
    fun findAllByBrandId(brandId: Long): List<ProductModel>
    fun existsByBrandIdAndName(brandId: Long, name: Name): Boolean
}
