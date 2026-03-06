package com.loopers.infrastructure.product

import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import org.springframework.stereotype.Repository

@Repository
class ProductStockRepositoryImpl(
    private val productStockJpaRepository: ProductStockJpaRepository,
) : ProductStockRepository {

    override fun findByProductIdForUpdate(productId: Long): ProductStock? {
        return productStockJpaRepository.findByProductIdForUpdate(productId)
    }

    override fun findByProductId(productId: Long): ProductStock? {
        return productStockJpaRepository.findByProductId(productId)
    }

    override fun findAllByProductIds(productIds: List<Long>): List<ProductStock> {
        return productStockJpaRepository.findAllByProductIds(productIds)
    }

    override fun save(productStock: ProductStock): ProductStock {
        return productStockJpaRepository.save(productStock)
    }
}
