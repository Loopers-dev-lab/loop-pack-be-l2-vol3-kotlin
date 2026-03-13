package com.loopers.domain.product

interface ProductRepository {
    fun save(product: Product): Product
    fun findById(id: Long): Product?
    fun findAll(): List<Product>
    fun findAll(sortType: ProductSortType, brandId: Long?): List<Product>
    fun findAllByBrandId(brandId: Long): List<Product>
    fun findAllByIds(ids: List<Long>): List<Product>
    fun existsByBrandIdAndStatus(brandId: Long, status: ProductStatus): Boolean
    fun deductStock(productId: Long, quantity: Int): Int
    fun restoreStock(productId: Long, quantity: Int): Int
    fun incrementLikeCount(productId: Long): Int
    fun decrementLikeCount(productId: Long): Int
}
