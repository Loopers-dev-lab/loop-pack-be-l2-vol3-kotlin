package com.loopers.application.event.product

import com.loopers.domain.product.ProductQueryInvalidator
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ProductQueryChangedEventHandler(
    private val productQueryInvalidator: ProductQueryInvalidator,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: ProductQueryChangedEvent) {
        val productIds = event.productIds.distinct()
        if (productIds.isNotEmpty()) {
            productQueryInvalidator.invalidateDetails(productIds)
        }

        event.brandIds
            .distinct()
            .forEach(productQueryInvalidator::invalidateListsByBrandId)
    }
}
