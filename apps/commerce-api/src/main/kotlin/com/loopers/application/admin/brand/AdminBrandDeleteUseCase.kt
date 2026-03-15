package com.loopers.application.admin.brand

import com.loopers.application.product.ProductQueryCache
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStockRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
class AdminBrandDeleteUseCase(
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val productQueryCache: ProductQueryCache,
) {
    @Transactional
    fun delete(brandId: Long, admin: String) {
        brandRepository.findById(brandId)
            ?: throw CoreException(ErrorType.BRAND_NOT_FOUND)

        // TODO: Brand UseCase가 Product/ProductStock Repository를 직접 조작하는 DDD 위반.
        //  추후 리팩토링.
        val products = productRepository.findAllByBrandId(brandId)
        val productIds = products.mapNotNull { it.id }
        if (productIds.isNotEmpty()) {
            productStockRepository.deleteAllByProductIds(productIds, admin)
            productRepository.deleteAllByBrandId(brandId, admin)
            registerDetailCacheEvictionAfterCommit(productIds)
        }

        brandRepository.delete(brandId, admin)
    }

    private fun registerDetailCacheEvictionAfterCommit(productIds: List<Long>) {
        if (
            !TransactionSynchronizationManager.isSynchronizationActive() ||
            !TransactionSynchronizationManager.isActualTransactionActive()
        ) {
            productQueryCache.evictDetails(productIds)
            return
        }

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    productQueryCache.evictDetails(productIds)
                }
            },
        )
    }
}
