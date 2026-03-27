package com.loopers.infrastructure.like

import com.loopers.support.event.user.ProductLikeCanceledEvent
import com.loopers.support.event.user.ProductLikeRegisteredEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ProductLikeEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: ProductLikeRegisteredEvent) {
        // API cache coherence is handled by query-time refresh from product_metrics.
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: ProductLikeCanceledEvent) {
        // API cache coherence is handled by query-time refresh from product_metrics.
    }
}
