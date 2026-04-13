package com.loopers.infrastructure.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.order.OrderRepository
import com.loopers.support.event.user.OrderCreatedEvent
import com.loopers.support.event.user.PaymentFailedEvent
import com.loopers.support.event.user.PaymentSucceededEvent
import com.loopers.support.event.user.CouponIssueRequestedEvent
import com.loopers.support.event.user.ProductDetailViewedEvent
import com.loopers.support.event.user.ProductLikeCanceledEvent
import com.loopers.support.event.user.ProductLikeRegisteredEvent
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class KafkaOutboxEventListener(
    private val kafkaOutboxJpaRepository: KafkaOutboxJpaRepository,
    private val orderRepository: OrderRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(event: ProductDetailViewedEvent) {
        persist(
            topic = "catalog-events",
            eventKey = event.productId.toString(),
            eventType = KafkaEventType.PRODUCT_DETAIL_VIEWED,
            aggregateId = event.productId,
            payload = KafkaOutboxMessagePayload(productId = event.productId),
        )
    }

    @EventListener
    fun handle(event: ProductLikeRegisteredEvent) {
        persist(
            topic = "catalog-events",
            eventKey = event.productId.toString(),
            eventType = KafkaEventType.PRODUCT_LIKE_REGISTERED,
            aggregateId = event.productId,
            payload = KafkaOutboxMessagePayload(productId = event.productId, userId = event.userId),
        )
    }

    @EventListener
    fun handle(event: ProductLikeCanceledEvent) {
        persist(
            topic = "catalog-events",
            eventKey = event.productId.toString(),
            eventType = KafkaEventType.PRODUCT_LIKE_CANCELED,
            aggregateId = event.productId,
            payload = KafkaOutboxMessagePayload(productId = event.productId, userId = event.userId),
        )
    }

    @EventListener
    fun handle(event: PaymentSucceededEvent) {
        val order = orderRepository.findById(event.orderId)
            ?: throw CoreException(ErrorType.ORDER_NOT_FOUND)

        persist(
            topic = "order-events",
            eventKey = event.orderId.toString(),
            eventType = KafkaEventType.PAYMENT_SUCCEEDED,
            aggregateId = event.orderId,
            payload = PaymentSucceededOutboxMessagePayload(
                paymentId = event.paymentId,
                orderId = event.orderId,
                userId = event.userId,
                items = order.items.map { item ->
                    PaymentSucceededOutboxMessageItemPayload(
                        productId = item.snapshot.productId,
                        quantity = item.quantity.value,
                        sellingPrice = item.snapshot.sellingPrice.toKrwLong(),
                    )
                },
            ),
        )
    }

    @EventListener
    fun handle(event: OrderCreatedEvent) {
        log.debug("Skip outbox publication for internal-only event. orderId={}", event.orderId)
    }

    @EventListener
    fun handle(event: PaymentFailedEvent) {
        log.debug("Skip outbox publication for internal-only event. paymentId={}", event.paymentId)
    }

    @EventListener
    fun handle(event: CouponIssueRequestedEvent) {
        persist(
            topic = "coupon-issue-requests",
            eventKey = event.couponId.toString(),
            eventType = KafkaEventType.COUPON_ISSUE_REQUESTED,
            aggregateId = event.couponId,
            payload = CouponIssueRequestedOutboxMessagePayload(
                requestId = event.requestId,
                couponId = event.couponId,
                userId = event.userId,
            ),
        )
    }

    private fun persist(
        topic: String,
        eventKey: String,
        eventType: KafkaEventType,
        aggregateId: Long,
        payload: Any,
    ) {
        val entity = KafkaOutboxEntity(
            topic = topic,
            eventKey = eventKey,
            eventType = eventType,
            aggregateId = aggregateId,
            payload = objectMapper.writeValueAsString(payload),
        )
        kafkaOutboxJpaRepository.saveAndFlush(entity)
    }
}
