package com.loopers.application.event

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class CatalogMetricsEventListener {

    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleCatalogEvent(event: CatalogEvent) {
        when (event) {
            is CatalogEvent.LikeAdded -> log.info("좋아요 추가: productId={}, userId={}", event.productId, event.userId)
            is CatalogEvent.LikeRemoved -> log.info("좋아요 제거: productId={}, userId={}", event.productId, event.userId)
            is CatalogEvent.ProductViewed -> log.info("상품 조회: productId={}, userId={}", event.productId, event.userId)
        }
    }
}
