package com.loopers.infrastructure.order

import com.loopers.domain.product.ProductQueryInvalidator
import com.loopers.support.event.user.OrderCreatedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class OrderCreatedEventListener(
    private val productQueryInvalidator: ProductQueryInvalidator,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: OrderCreatedEvent) {
        productQueryInvalidator.invalidateDetails(event.productIds.distinct())
    }
}
