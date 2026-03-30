package com.loopers.application.event

import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.outbox.model.CatalogOutbox
import com.loopers.domain.outbox.model.CatalogOutboxEventType
import com.loopers.domain.outbox.repository.CatalogOutboxRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class CatalogMetricsEventListener(
    private val catalogOutboxRepository: CatalogOutboxRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleProductViewed(event: CatalogEvent.ProductViewed) {
        val outbox = CatalogOutbox(
            eventType = CatalogOutboxEventType.PRODUCT_VIEWED,
            productId = ProductId(event.productId),
            userId = event.userId?.let { UserId(it) },
        )
        catalogOutboxRepository.save(outbox)
        log.debug("CatalogOutbox 기록: eventType={}, productId={}", outbox.eventType, outbox.productId)
    }
}
