package com.loopers.application.event

import com.loopers.domain.outbox.model.CatalogOutbox
import com.loopers.domain.outbox.repository.CatalogOutboxRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class CatalogMetricsEventListener(
    private val catalogOutboxRepository: CatalogOutboxRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleCatalogEvent(event: CatalogEvent) {
        val outbox = when (event) {
            is CatalogEvent.LikeAdded -> CatalogOutbox(
                eventType = "LIKE_ADDED",
                productId = event.productId,
                userId = event.userId,
            )
            is CatalogEvent.LikeRemoved -> CatalogOutbox(
                eventType = "LIKE_REMOVED",
                productId = event.productId,
                userId = event.userId,
            )
            is CatalogEvent.ProductViewed -> CatalogOutbox(
                eventType = "PRODUCT_VIEWED",
                productId = event.productId,
                userId = event.userId,
            )
        }
        catalogOutboxRepository.save(outbox)
        log.info("CatalogOutbox 기록: eventType={}, productId={}", outbox.eventType, outbox.productId)
    }
}
