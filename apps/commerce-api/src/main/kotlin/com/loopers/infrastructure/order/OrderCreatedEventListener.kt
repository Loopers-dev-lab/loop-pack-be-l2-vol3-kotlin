package com.loopers.infrastructure.order

import com.loopers.domain.product.ProductQueryInvalidator
import com.loopers.domain.queue.EntryTokenRepository
import com.loopers.support.event.user.OrderCreatedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class OrderCreatedEventListener(
    private val productQueryInvalidator: ProductQueryInvalidator,
    private val entryTokenRepository: EntryTokenRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: OrderCreatedEvent) {
        productQueryInvalidator.invalidateDetails(event.productIds.distinct())
        if (event.hasEntryToken) {
            runCatching { entryTokenRepository.delete(event.userId) }
                .onFailure { log.warn("Failed to delete entry token for userId={}", event.userId, it) }
        }
    }
}
