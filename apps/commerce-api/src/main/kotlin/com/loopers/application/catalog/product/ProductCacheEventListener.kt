package com.loopers.application.catalog.product

import com.loopers.domain.catalog.product.repository.ProductCacheRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ProductCacheEventListener(
    private val productCacheRepository: ProductCacheRepository,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleDetailUpdated(event: ProductCacheEvent.DetailUpdated) {
        productCacheRepository.saveProductDetail(event.product)
        if (event.evictList) {
            productCacheRepository.evictProductList(event.product.refBrandId)
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleDetailEvicted(event: ProductCacheEvent.DetailEvicted) {
        productCacheRepository.evictProductDetail(event.productId)
        productCacheRepository.evictProductList(event.brandId)
    }
}
