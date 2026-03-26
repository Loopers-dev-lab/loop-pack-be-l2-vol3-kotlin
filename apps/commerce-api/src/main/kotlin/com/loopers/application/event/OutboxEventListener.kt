package com.loopers.application.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.outbox.OutboxEvent
import com.loopers.domain.outbox.OutboxEventRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class OutboxEventListener(
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        const val TOPIC_CATALOG_EVENTS = "catalog-events"
        const val TOPIC_ORDER_EVENTS = "order-events"
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleOrderCreated(event: OrderCreatedEvent) {
        saveOutbox(
            eventId = event.eventId,
            eventType = "ORDER_CREATED",
            aggregateId = event.orderId.toString(),
            topic = TOPIC_ORDER_EVENTS,
            partitionKey = event.orderId.toString(),
            payload = objectMapper.writeValueAsString(event),
            occurredAt = event.occurredAt,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleOrderCompleted(event: OrderCompletedEvent) {
        saveOutbox(
            eventId = event.eventId,
            eventType = "ORDER_COMPLETED",
            aggregateId = event.orderId.toString(),
            topic = TOPIC_ORDER_EVENTS,
            partitionKey = event.orderId.toString(),
            payload = objectMapper.writeValueAsString(event),
            occurredAt = event.occurredAt,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleProductLiked(event: ProductLikedEvent) {
        saveOutbox(
            eventId = event.eventId,
            eventType = "PRODUCT_LIKED",
            aggregateId = event.productId.toString(),
            topic = TOPIC_CATALOG_EVENTS,
            partitionKey = event.productId.toString(),
            payload = objectMapper.writeValueAsString(event),
            occurredAt = event.occurredAt,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleProductUnliked(event: ProductUnlikedEvent) {
        saveOutbox(
            eventId = event.eventId,
            eventType = "PRODUCT_UNLIKED",
            aggregateId = event.productId.toString(),
            topic = TOPIC_CATALOG_EVENTS,
            partitionKey = event.productId.toString(),
            payload = objectMapper.writeValueAsString(event),
            occurredAt = event.occurredAt,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleProductViewed(event: ProductViewedEvent) {
        saveOutbox(
            eventId = event.eventId,
            eventType = "PRODUCT_VIEWED",
            aggregateId = event.productId.toString(),
            topic = TOPIC_CATALOG_EVENTS,
            partitionKey = event.productId.toString(),
            payload = objectMapper.writeValueAsString(event),
            occurredAt = event.occurredAt,
        )
    }

    private fun saveOutbox(
        eventId: String,
        eventType: String,
        aggregateId: String,
        topic: String,
        partitionKey: String,
        payload: String,
        occurredAt: java.time.ZonedDateTime,
    ) {
        val outboxEvent = OutboxEvent.create(
            eventId = eventId,
            eventType = eventType,
            aggregateId = aggregateId,
            topic = topic,
            partitionKey = partitionKey,
            payload = payload,
            occurredAt = occurredAt,
        )
        outboxEventRepository.save(outboxEvent)
    }
}
