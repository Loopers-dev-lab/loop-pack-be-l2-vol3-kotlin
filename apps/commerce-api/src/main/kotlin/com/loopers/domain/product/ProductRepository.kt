package com.loopers.domain.product

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductRepository {
    fun save(product: Product): Product
    fun findAll(brandId: Long?, pageable: Pageable): Page<Product>
    fun findById(id: Long): Product?
    fun findAllByIds(ids: List<Long>): List<Product>
}
