package com.loopers.infrastructure.product

import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import org.springframework.stereotype.Repository

@Repository
class ProductStockRepositoryImpl(
    private val productStockJpaRepository: ProductStockJpaRepository,
    private val productStockMapper: ProductStockMapper,
) : ProductStockRepository {

    override fun save(stock: ProductStock, admin: String): ProductStock {
        val entity = if (stock.id != null) {
            val existing = productStockJpaRepository.findById(stock.id).orElseThrow()
            existing.quantity = stock.quantity.value
            existing.updateBy(admin)
            existing
        } else {
            productStockMapper.toEntity(stock, admin)
        }
        return productStockMapper.toDomain(productStockJpaRepository.saveAndFlush(entity))
    }

    override fun findByProductId(productId: Long): ProductStock? {
        return productStockJpaRepository.findByProductIdAndDeletedAtIsNull(productId)
            ?.let { productStockMapper.toDomain(it) }
    }

    override fun findAllByProductIdIn(productIds: List<Long>): List<ProductStock> {
        if (productIds.isEmpty()) return emptyList()
        return productStockJpaRepository.findAllByProductIdInAndDeletedAtIsNull(productIds)
            .map { productStockMapper.toDomain(it) }
    }

    override fun deleteByProductId(productId: Long, admin: String) {
        val entity = productStockJpaRepository.findByProductIdAndDeletedAtIsNull(productId) ?: return
        entity.deleteBy(admin)
        productStockJpaRepository.saveAndFlush(entity)
    }

    override fun deleteAllByProductIds(productIds: List<Long>, admin: String) {
        if (productIds.isEmpty()) return
        val entities = productStockJpaRepository.findAllByProductIdInAndDeletedAtIsNull(productIds)
        entities.forEach { it.deleteBy(admin) }
        productStockJpaRepository.saveAllAndFlush(entities)
    }
}
