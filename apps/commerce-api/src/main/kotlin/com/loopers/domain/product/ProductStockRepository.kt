package com.loopers.domain.product

interface ProductStockRepository {
    fun findByProductIdForUpdate(productId: Long): ProductStock?
    fun findByProductId(productId: Long): ProductStock?
    fun findAllByProductIds(productIds: List<Long>): List<ProductStock>
    fun save(productStock: ProductStock): ProductStock
}
