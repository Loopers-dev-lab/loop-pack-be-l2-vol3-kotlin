package com.loopers.application.event

import com.loopers.application.metrics.ProductMetricsUpdater
import com.loopers.application.ranking.RankingUpdater
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ProductAggregationListener(
    private val productMetricsUpdater: ProductMetricsUpdater,
    private val rankingUpdater: RankingUpdater,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: ProductViewedEvent) {
        productMetricsUpdater.increaseViewCount(event.productId, event.occurredAt)
        rankingUpdater.applyViewed(event.productId, event.occurredAt)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: ProductLikeChangedEvent) {
        when {
            event.delta > 0 -> productMetricsUpdater.increaseLikeCount(event.productId, event.occurredAt)
            event.delta < 0 -> productMetricsUpdater.decreaseLikeCount(event.productId, event.occurredAt)
        }
        rankingUpdater.applyLikeChanged(event.productId, event.delta, event.occurredAt)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: OrderPaidEvent) {
        event.items.forEach { item ->
            productMetricsUpdater.increaseSalesCount(item.productId, item.quantity, event.occurredAt)
            rankingUpdater.applyOrdered(item.productId, item.quantity, event.occurredAt)
        }
    }
}
