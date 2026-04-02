package com.loopers.application.like

import com.loopers.application.event.LikeActionType
import com.loopers.application.event.LikeChangedEvent
import com.loopers.application.product.ProductCacheStore
import com.loopers.domain.product.ProductService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class LikeAggregationEventListener(
    private val productService: ProductService,
    private val productCacheStore: ProductCacheStore,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: LikeChangedEvent) {
        runCatching {
            when (event.actionType) {
                LikeActionType.LIKE -> productService.incrementLikesCount(event.productId)
                LikeActionType.UNLIKE -> productService.decrementLikesCount(event.productId)
            }
            productCacheStore.evictProductDetail(event.productId)
            productCacheStore.evictProductList()
        }.onFailure {
            log.warn(
                "like_aggregation_failed actionType={} userId={} productId={}",
                event.actionType,
                event.userId,
                event.productId,
                it,
            )
        }
    }
}
