package com.loopers.domain.catalog.product.repository

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.product.ProductSort
import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.common.vo.BrandId
import com.loopers.domain.common.vo.ProductId

interface ProductRepository {
    fun save(product: Product): Product
    fun saveAll(products: List<Product>): List<Product>
    fun findById(id: ProductId): Product?
    fun findByIdForUpdate(id: ProductId): Product?
    fun findAll(page: Int, size: Int): PageResult<Product>
    fun findAllIncludeDeleted(page: Int, size: Int): PageResult<Product>
    fun findActiveProducts(brandId: BrandId?, sort: ProductSort, page: Int, size: Int): PageResult<Product>
    fun findAllByBrandId(brandId: BrandId): List<Product>
    fun findAllByIds(ids: List<ProductId>): List<Product>
    fun findAllByIdsForUpdate(ids: List<ProductId>): List<Product>
}
