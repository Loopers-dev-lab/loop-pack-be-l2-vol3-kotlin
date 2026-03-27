package com.loopers.domain.useractionlog

import com.loopers.domain.event.ProductViewedEvent
import com.loopers.domain.productlike.event.ProductLikedEvent
import com.loopers.domain.productlike.event.ProductUnlikedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.LocalDate

@Component
class ProductUserActionLogEventListener(
    private val userActionLogPersistenceService: UserActionLogPersistenceService,
) {
    companion object {
        private val log = LoggerFactory.getLogger(ProductUserActionLogEventListener::class.java)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
    fun onProductViewed(event: ProductViewedEvent) {
        appendSafely(
            UserActionLogAppendCommand(
                actionType = "product.viewed",
                actorUserId = event.userId,
                targetId = event.productId.toString(),
                payload = "",
                dedupeKey = event.dedupeKey,
                partitionDate = LocalDate.now(),
            ),
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
    fun onProductLiked(event: ProductLikedEvent) {
        appendSafely(
            UserActionLogAppendCommand(
                actionType = "product.liked",
                actorUserId = event.userId,
                targetId = event.productId.toString(),
                payload = "",
                dedupeKey = event.dedupeKey,
                partitionDate = LocalDate.now(),
            ),
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
    fun onProductUnliked(event: ProductUnlikedEvent) {
        appendSafely(
            UserActionLogAppendCommand(
                actionType = "product.unliked",
                actorUserId = event.userId,
                targetId = event.productId.toString(),
                payload = "",
                dedupeKey = event.dedupeKey,
                partitionDate = LocalDate.now(),
            ),
        )
    }

    private fun appendSafely(command: UserActionLogAppendCommand) {
        try {
            userActionLogPersistenceService.appendIfAbsent(command)
        } catch (e: Exception) {
            log.error(
                "Failed to persist product ancillary user-action-log. actionType={}, dedupeKey={}",
                command.actionType,
                command.dedupeKey,
                e,
            )
        }
    }
}
