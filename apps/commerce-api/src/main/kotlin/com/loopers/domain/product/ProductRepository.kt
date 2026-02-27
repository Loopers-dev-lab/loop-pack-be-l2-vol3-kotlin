package com.loopers.domain.product

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult

interface ProductRepository {
    fun save(product: Product): Product
    fun findAll(brandId: Long?, pageQuery: PageQuery): PageResult<Product>
    fun findById(id: Long): Product?
    fun findAllByIds(ids: List<Long>): List<Product>
    fun findAllByBrandId(brandId: Long): List<Product>
}
