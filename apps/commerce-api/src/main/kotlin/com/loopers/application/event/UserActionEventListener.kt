package com.loopers.application.event

import com.loopers.domain.event.LikeCancelledEvent
import com.loopers.domain.event.LikeCreatedEvent
import com.loopers.domain.event.OrderCreatedEvent
import com.loopers.domain.event.PaymentApprovedEvent
import com.loopers.domain.event.PaymentFailedEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class UserActionEventListener {

    private val log = LoggerFactory.getLogger("UserActionLog")

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleLikeCreated(event: LikeCreatedEvent) {
        log.info(
            "action=LIKE target=PRODUCT userId={} targetId={} occurredAt={}",
            event.userId,
            event.productId,
            event.occurredAt,
        )
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleLikeCancelled(event: LikeCancelledEvent) {
        log.info(
            "action=UNLIKE target=PRODUCT userId={} targetId={} occurredAt={}",
            event.userId,
            event.productId,
            event.occurredAt,
        )
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleOrderCreated(event: OrderCreatedEvent) {
        log.info(
            "action=ORDER target=ORDER userId={} targetId={} totalAmount={} productIds={} occurredAt={}",
            event.userId,
            event.orderId,
            event.totalAmount,
            event.productIds,
            event.occurredAt,
        )
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePaymentApproved(event: PaymentApprovedEvent) {
        log.info(
            "action=PAYMENT_APPROVED target=PAYMENT userId={} targetId={} amount={} orderId={} occurredAt={}",
            event.userId,
            event.paymentId,
            event.amount,
            event.orderId,
            event.occurredAt,
        )
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePaymentFailed(event: PaymentFailedEvent) {
        log.info(
            "action=PAYMENT_FAILED target=PAYMENT userId={} targetId={} orderId={} reason={} occurredAt={}",
            event.userId,
            event.paymentId,
            event.orderId,
            event.reason,
            event.occurredAt,
        )
    }
}
