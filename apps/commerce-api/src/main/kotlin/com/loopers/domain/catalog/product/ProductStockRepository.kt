package com.loopers.domain.catalog.product

interface ProductStockRepository {
    fun save(stock: ProductStock): ProductStock
    fun findByProductId(productId: Long): ProductStock?
    fun findByProductIdForUpdate(productId: Long): ProductStock?
}
