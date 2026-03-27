package com.loopers.infrastructure.like

import com.loopers.application.user.like.ProductLikeCanceledEvent
import com.loopers.application.user.like.ProductLikeRegisteredEvent
import com.loopers.domain.product.ProductQueryInvalidator
import com.loopers.domain.product.ProductRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ProductLikeEventListener(
    private val productRepository: ProductRepository,
    private val productQueryInvalidator: ProductQueryInvalidator,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: ProductLikeRegisteredEvent) {
        runSafely(
            action = "increment product like count after like registered",
            productId = event.productId,
        ) {
            productRepository.incrementLikeCount(event.productId)
        }
        runSafely(
            action = "invalidate product detail cache after like registered",
            productId = event.productId,
        ) {
            productQueryInvalidator.invalidateDetails(listOf(event.productId))
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: ProductLikeCanceledEvent) {
        runSafely(
            action = "decrement product like count after like canceled",
            productId = event.productId,
        ) {
            productRepository.decrementLikeCount(event.productId)
        }
        runSafely(
            action = "invalidate product detail cache after like canceled",
            productId = event.productId,
        ) {
            productQueryInvalidator.invalidateDetails(listOf(event.productId))
        }
    }

    private fun runSafely(
        action: String,
        productId: Long,
        block: () -> Unit,
    ) {
        runCatching(block)
            .onFailure { exception ->
                log.warn(
                    "Failed to {}. productId={}",
                    action,
                    productId,
                    exception,
                )
            }
    }
}
