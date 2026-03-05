package com.loopers.domain.product

import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult

interface ProductRepository {
    fun save(product: Product): Product
    fun findAll(brandId: Long?, pageQuery: PageQuery): PageResult<Product>
    fun findById(id: Long): Product?
    fun findAllByIds(ids: List<Long>): List<Product>
    fun findAllByIdsWithLock(ids: List<Long>): List<Product>
    fun findAllByBrandId(brandId: Long): List<Product>
    fun incrementLikeCount(productId: Long)
    fun decrementLikeCount(productId: Long)
}
