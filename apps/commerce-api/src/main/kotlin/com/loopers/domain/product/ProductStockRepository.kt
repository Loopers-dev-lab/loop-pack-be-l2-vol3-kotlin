package com.loopers.domain.product

interface ProductStockRepository {
    fun save(stock: ProductStock, admin: String): ProductStock
    fun findByProductId(productId: Long): ProductStock?
    fun findAllByProductIdIn(productIds: List<Long>): List<ProductStock>
    fun deleteByProductId(productId: Long, admin: String)
    fun deleteAllByProductIds(productIds: List<Long>, admin: String)
}
