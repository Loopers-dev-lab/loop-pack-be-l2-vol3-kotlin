package com.loopers.domain.product

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductRepository {
    fun save(product: Product): Product
    fun findById(id: Long): Product?
    fun findByIdWithLock(id: Long): Product?
    fun findAll(pageable: Pageable): Page<Product>
    fun findAllByBrandId(brandId: Long, pageable: Pageable): Page<Product>
    fun findAllByBrandId(brandId: Long): List<Product>
}
