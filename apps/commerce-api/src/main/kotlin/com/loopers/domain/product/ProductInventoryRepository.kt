package com.loopers.domain.product

interface ProductInventoryRepository {
    fun save(inventory: ProductInventoryModel): ProductInventoryModel
    fun findByProductId(productId: Long): ProductInventoryModel?
}
