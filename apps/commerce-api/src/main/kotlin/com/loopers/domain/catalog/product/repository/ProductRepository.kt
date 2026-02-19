package com.loopers.domain.catalog.product.repository

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.product.entity.Product
import com.loopers.domain.catalog.product.ProductSort

interface ProductRepository {
    fun save(product: Product): Product
    fun findById(id: Long): Product?
    fun findAll(page: Int, size: Int): PageResult<Product>
    fun findActiveProducts(brandId: Long?, sort: ProductSort, page: Int, size: Int): PageResult<Product>
    fun findAllByBrandId(brandId: Long): List<Product>
    fun findAllByIds(ids: List<Long>): List<Product>
}
