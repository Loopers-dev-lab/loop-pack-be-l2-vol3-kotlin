package com.loopers.application.event

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class UserActionLoggingEventListener(
    private val userActionLogWriter: UserActionLogWriter,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleOrderCompleted(event: OrderCompletedEvent) {
        writeSafely(
            UserActionLogCommand(
                userId = event.userId,
                actionType = UserActionType.ORDER_CREATED,
                targetId = event.orderId,
                metadata = mapOf("totalAmount" to event.totalAmount),
            ),
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleLikeChanged(event: LikeChangedEvent) {
        val actionType = when (event.actionType) {
            LikeActionType.LIKE -> UserActionType.PRODUCT_LIKED
            LikeActionType.UNLIKE -> UserActionType.PRODUCT_UNLIKED
        }
        writeSafely(
            UserActionLogCommand(
                userId = event.userId,
                actionType = actionType,
                targetId = event.productId,
            ),
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleProductViewed(event: ProductViewedEvent) {
        val actionType = when (event.actionType) {
            ProductViewActionType.PRODUCT_DETAIL_VIEWED -> UserActionType.PRODUCT_DETAIL_VIEWED
            ProductViewActionType.PRODUCT_LIST_VIEWED -> UserActionType.PRODUCT_LIST_VIEWED
        }
        writeSafely(
            UserActionLogCommand(
                userId = null,
                actionType = actionType,
                targetId = event.productId,
            ),
        )
    }

    private fun writeSafely(command: UserActionLogCommand) {
        runCatching { userActionLogWriter.write(command) }
            .onFailure {
                log.warn(
                    "user_action_logging_failed actionType={} userId={} targetId={}",
                    command.actionType,
                    command.userId,
                    command.targetId,
                    it,
                )
            }
    }
}
