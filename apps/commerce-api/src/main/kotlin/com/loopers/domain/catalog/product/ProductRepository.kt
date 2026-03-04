package com.loopers.domain.catalog.product

interface ProductRepository {
    fun save(product: Product): Product
    fun findById(id: Long): Product?
    fun findByIdForUpdate(id: Long): Product?
    fun findAll(condition: ProductSearchCondition): List<Product>
    fun findAllByBrandId(brandId: Long): List<Product>
    fun deleteById(id: Long)
    fun deleteAllByBrandId(brandId: Long)
    fun incrementLikeCountAtomic(id: Long): Boolean
    fun decrementLikeCountAtomic(id: Long): Boolean
}
