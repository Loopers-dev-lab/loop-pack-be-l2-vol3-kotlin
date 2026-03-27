package com.loopers.infrastructure.activity

import com.loopers.support.event.user.OrderCreatedEvent
import com.loopers.support.event.user.PaymentFailedEvent
import com.loopers.support.event.user.PaymentSucceededEvent
import com.loopers.support.event.user.ProductDetailViewedEvent
import com.loopers.support.event.user.ProductLikeCanceledEvent
import com.loopers.support.event.user.ProductLikeRegisteredEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class UserActivityEventLogger {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener
    fun handle(event: ProductDetailViewedEvent) {
        log.info(
            "User activity recorded. action=product_detail_viewed, productId={}",
            event.productId,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: ProductLikeRegisteredEvent) {
        log.info(
            "User activity recorded. action=product_like_registered, userId={}, productId={}",
            event.userId,
            event.productId,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: ProductLikeCanceledEvent) {
        log.info(
            "User activity recorded. action=product_like_canceled, userId={}, productId={}",
            event.userId,
            event.productId,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: OrderCreatedEvent) {
        log.info(
            "User activity recorded. action=order_created, orderId={}, userId={}, productIds={}",
            event.orderId,
            event.userId,
            event.productIds,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: PaymentSucceededEvent) {
        log.info(
            "User activity recorded. action=payment_succeeded, paymentId={}, orderId={}, userId={}",
            event.paymentId,
            event.orderId,
            event.userId,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: PaymentFailedEvent) {
        log.info(
            "User activity recorded. action=payment_failed, paymentId={}, orderId={}, userId={}, reasonCode={}",
            event.paymentId,
            event.orderId,
            event.userId,
            event.reasonCode,
        )
    }
}
