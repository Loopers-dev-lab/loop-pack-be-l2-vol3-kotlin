package com.loopers.infrastructure.product

import com.loopers.domain.product.ProductInventoryModel
import com.loopers.domain.product.ProductInventoryRepository
import org.springframework.stereotype.Repository

@Repository
class ProductInventoryImpl(
    private val productInventoryJpaRepository: ProductInventoryJpaRepository,
) : ProductInventoryRepository {
    override fun save(inventory: ProductInventoryModel): ProductInventoryModel {
        return productInventoryJpaRepository.save(inventory)
    }

    override fun findByProductId(productId: Long): ProductInventoryModel? {
        return productInventoryJpaRepository.findByProductId(productId)
    }

    override fun findByProductIdForUpdate(productId: Long): ProductInventoryModel? {
        return productInventoryJpaRepository.findByProductIdForUpdate(productId)
    }
}
