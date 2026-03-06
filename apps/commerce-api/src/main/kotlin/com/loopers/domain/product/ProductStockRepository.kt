package com.loopers.domain.product

interface ProductStockRepository {
    fun findByProductIdForUpdate(productId: Long): ProductStock?
    fun findByProductId(productId: Long): ProductStock?
    fun save(productStock: ProductStock): ProductStock
}
