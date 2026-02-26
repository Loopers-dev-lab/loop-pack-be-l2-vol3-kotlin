package com.loopers.domain.product

import com.loopers.support.PageResult

interface ProductRepository {
    fun findByIdOrNull(id: Long): Product?
    fun findActiveByIdOrNull(id: Long): Product?
    fun findAllByCondition(condition: ProductSearchCondition): PageResult<Product>
    fun findAllActiveByBrandId(brandId: Long): List<Product>
    fun findAllActiveByIds(ids: List<Long>): List<Product>
    fun save(product: Product): Product
    fun saveAll(products: List<Product>): List<Product>
}
