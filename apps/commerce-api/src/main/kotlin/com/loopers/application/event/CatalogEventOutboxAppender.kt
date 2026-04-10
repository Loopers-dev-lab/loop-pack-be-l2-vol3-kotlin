package com.loopers.application.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.config.kafka.event.CatalogEventMessage
import com.loopers.config.kafka.event.CatalogEventType
import com.loopers.domain.outbox.OutboxEventModel
import com.loopers.infrastructure.outbox.OutboxEventJpaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.util.UUID

@Component
class CatalogEventOutboxAppender(
    private val outboxEventJpaRepository: OutboxEventJpaRepository,
    private val objectMapper: ObjectMapper,
    @Value("\${step2.kafka.catalog-topic}") private val catalogTopic: String,
) {
    @EventListener
    fun appendLikeChanged(event: LikeChangedEvent) {
        val message = CatalogEventMessage(
            eventId = UUID.randomUUID().toString(),
            productId = event.productId,
            eventType = CatalogEventType.LIKE_CHANGED,
            delta = if (event.actionType == LikeActionType.LIKE) 1 else -1,
            version = System.currentTimeMillis(),
            occurredAt = ZonedDateTime.now(),
        )
        append(message)
    }

    @EventListener
    fun appendOrderCompleted(event: OrderCompletedEvent) {
        event.orderItems.forEach { item ->
            val message = CatalogEventMessage(
                eventId = UUID.randomUUID().toString(),
                productId = item.productId,
                eventType = CatalogEventType.ORDER_COMPLETED,
                delta = item.quantity.toLong(),
                version = System.currentTimeMillis(),
                occurredAt = ZonedDateTime.now(),
            )
            append(message)
        }
    }

    @EventListener
    fun appendProductViewed(event: ProductViewedEvent) {
        val productId = event.productId ?: return
        val message = CatalogEventMessage(
            eventId = UUID.randomUUID().toString(),
            productId = productId,
            eventType = CatalogEventType.PRODUCT_VIEWED,
            delta = 1,
            version = System.currentTimeMillis(),
            occurredAt = ZonedDateTime.now(),
        )
        append(message)
    }

    private fun append(message: CatalogEventMessage) {
        outboxEventJpaRepository.save(
            OutboxEventModel(
                eventId = message.eventId,
                topic = catalogTopic,
                partitionKey = partitionKey(message.productId),
                payload = objectMapper.writeValueAsString(message),
            ),
        )
    }

    private fun partitionKey(productId: Long): String = "product:$productId"
}
