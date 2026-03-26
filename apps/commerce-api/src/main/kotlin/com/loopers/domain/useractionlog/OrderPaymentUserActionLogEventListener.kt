package com.loopers.domain.useractionlog

import com.loopers.domain.order.event.OrderCreatedEvent
import com.loopers.domain.payment.event.PaymentCallbackProcessedEvent
import com.loopers.domain.payment.event.PaymentRequestedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.LocalDate

@Component
class OrderPaymentUserActionLogEventListener(
    private val userActionLogPersistenceService: UserActionLogPersistenceService,
) {
    companion object {
        private val log = LoggerFactory.getLogger(OrderPaymentUserActionLogEventListener::class.java)
        private const val SYSTEM_ACTOR_USER_ID = -1L
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
    fun onOrderCreated(event: OrderCreatedEvent) {
        // Log the order creation with line items
        val payload = event.lineItems.joinToString(";") { item ->
            "productId=${item.productId},quantity=${item.quantity}"
        }
        appendSafely(
            UserActionLogAppendCommand(
                actionType = "order.created",
                actorUserId = SYSTEM_ACTOR_USER_ID,
                targetId = event.orderId.toString(),
                payload = payload,
                dedupeKey = event.dedupeKey,
                partitionDate = LocalDate.now(),
            ),
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
    fun onPaymentRequested(event: PaymentRequestedEvent) {
        appendSafely(
            UserActionLogAppendCommand(
                actionType = "payment.requested",
                actorUserId = event.userId,
                targetId = event.orderId.toString(),
                payload = "transactionId=${event.transactionId},amount=${event.amount},status=${event.receiptStatus}",
                dedupeKey = event.dedupeKey,
                partitionDate = LocalDate.now(),
            ),
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
    fun onPaymentCallbackProcessed(event: PaymentCallbackProcessedEvent) {
        appendSafely(
            UserActionLogAppendCommand(
                actionType = "payment.callback.processed",
                actorUserId = SYSTEM_ACTOR_USER_ID,
                targetId = event.orderId.toString(),
                payload =
                    "transactionId=${event.transactionId},status=${event.status},amount=${event.amount},reason=${event.reason}",
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
                "Failed to persist order/payment ancillary user-action-log. actionType={}, dedupeKey={}",
                command.actionType,
                command.dedupeKey,
                e,
            )
        }
    }
}
