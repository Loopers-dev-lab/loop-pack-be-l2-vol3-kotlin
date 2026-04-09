package com.loopers.domain.product

interface ProductRepository {
    fun save(product: Product): Long
    fun findById(id: Long): Product?
    fun findByIdForUpdate(id: Long): Product?
    fun decreaseStock(id: Long, quantity: Int): Int
    fun increaseStock(id: Long, quantity: Int): Int
    fun incrementLikeCount(id: Long): Int
    fun decrementLikeCount(id: Long): Int
    fun softDeleteByBrandId(brandId: Long): Int
    fun findAllActive(brandId: Long?, sortType: ProductSortType): List<Product>
    fun findAll(brandId: Long?): List<Product>
}
