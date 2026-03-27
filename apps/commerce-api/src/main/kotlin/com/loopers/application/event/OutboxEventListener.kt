package com.loopers.application.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.config.kafka.KafkaTopics
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
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleOrderCreated(event: OrderCreatedEvent) {
        saveOutbox(
            eventId = event.eventId,
            eventType = "ORDER_CREATED",
            aggregateId = event.orderId.toString(),
            topic = KafkaTopics.ORDER_EVENTS,
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
            topic = KafkaTopics.ORDER_EVENTS,
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
            topic = KafkaTopics.CATALOG_EVENTS,
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
            topic = KafkaTopics.CATALOG_EVENTS,
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
            topic = KafkaTopics.CATALOG_EVENTS,
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
        // eventType을 payload에 포함시켜 relay가 payload를 변조하지 않도록 한다
        val payloadMap = objectMapper.readValue(payload, Map::class.java).toMutableMap()
        payloadMap["eventType"] = eventType
        val enrichedPayload = objectMapper.writeValueAsString(payloadMap)

        val outboxEvent = OutboxEvent.create(
            eventId = eventId,
            eventType = eventType,
            aggregateId = aggregateId,
            topic = topic,
            partitionKey = partitionKey,
            payload = enrichedPayload,
            occurredAt = occurredAt,
        )
        outboxEventRepository.save(outboxEvent)
    }
}
