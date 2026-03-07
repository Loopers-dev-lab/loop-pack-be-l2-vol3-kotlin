package com.loopers.domain.product

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductRepository {
    fun save(product: Product): Product
    fun findById(id: Long): Product?
    fun findByBrandId(brandId: Long): List<Product>
    fun findAll(): List<Product>
    fun findAllForUser(pageable: Pageable, brandId: Long?): Page<Product>
    fun findAllForAdmin(pageable: Pageable, brandId: Long?): Page<Product>
    fun findByIds(ids: List<Long>): List<Product>
    fun decreaseStock(productId: Long, quantity: Int): Int
    fun increaseLikeCount(productId: Long): Int
    fun decreaseLikeCount(productId: Long): Int
}
