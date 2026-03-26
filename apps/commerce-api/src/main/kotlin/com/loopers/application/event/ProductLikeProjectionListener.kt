package com.loopers.application.event

import com.loopers.application.product.ProductCacheStore
import com.loopers.domain.product.ProductLikeCountUpdater
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ProductLikeProjectionListener(
    private val productLikeCountUpdater: ProductLikeCountUpdater,
    private val productCacheStore: ProductCacheStore,
) {

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: ProductLikeChangedEvent) {
        when {
            event.delta > 0 -> productLikeCountUpdater.increase(event.productId)
            event.delta < 0 -> productLikeCountUpdater.decrease(event.productId)
        }
        productCacheStore.evictDetail(event.productId)
        productCacheStore.evictList()
        productCacheStore.evictList(event.brandId)
    }
}
