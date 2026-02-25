package com.loopers.domain.catalog.product.repository

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.product.ProductSort
import com.loopers.domain.catalog.product.model.Product

interface ProductRepository {
    fun save(product: Product): Product
    fun findById(id: Long): Product?
    fun findByIdIncludeDeleted(id: Long): Product?
    fun findByIdForUpdate(id: Long): Product?
    fun findAll(page: Int, size: Int): PageResult<Product>
    fun findAllIncludeDeleted(page: Int, size: Int): PageResult<Product>
    fun findActiveProducts(brandId: Long?, sort: ProductSort, page: Int, size: Int): PageResult<Product>
    fun findAllByBrandId(brandId: Long): List<Product>
    fun findAllByIds(ids: List<Long>): List<Product>
    fun findAllByIdsForUpdate(ids: List<Long>): List<Product>
    fun saveAll(products: List<Product>): List<Product>
}
