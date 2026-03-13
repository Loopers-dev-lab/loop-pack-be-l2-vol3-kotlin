package com.loopers.application.product

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ProductCacheEventListener(
    private val productCachePort: ProductCachePort,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleCacheEvict(event: ProductCacheEvictEvent) {
        productCachePort.evictProductDetail(event.productId)
    }
}
