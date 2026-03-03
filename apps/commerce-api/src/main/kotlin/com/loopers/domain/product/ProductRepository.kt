package com.loopers.domain.product

import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse

interface ProductRepository {
    fun save(product: Product, admin: String): Product
    fun delete(productId: Long, admin: String)
    fun findById(id: Long): Product?
    fun findAll(pageRequest: PageRequest, brandId: Long? = null): PageResponse<Product>
    fun findAllActive(pageRequest: PageRequest, brandId: Long?, sort: Product.SortType?): PageResponse<Product>
    fun findAllByBrandId(brandId: Long): List<Product>
    fun findAllByIdIn(ids: List<Long>): List<Product>
    fun deleteAllByBrandId(brandId: Long, admin: String)
}
