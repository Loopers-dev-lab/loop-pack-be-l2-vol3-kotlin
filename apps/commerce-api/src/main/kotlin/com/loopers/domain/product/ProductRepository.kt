package com.loopers.domain.product

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductRepository {

    fun findById(id: Long): Product?

    fun findByBrandId(brandId: Long): List<Product>

    fun findWithPaging(brandId: Long?, pageable: Pageable): Page<Product>

    fun save(product: Product): Product
}
