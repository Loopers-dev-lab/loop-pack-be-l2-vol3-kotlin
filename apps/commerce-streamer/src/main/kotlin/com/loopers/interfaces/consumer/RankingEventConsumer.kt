package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.config.kafka.KafkaConfig
import com.loopers.domain.deadletter.FailedEventService
import com.loopers.domain.metrics.OrderItemMetrics
import com.loopers.domain.ranking.RankingEventService
import com.loopers.domain.ranking.ViewCount
import com.loopers.infrastructure.order.OrderJpaRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class RankingEventConsumer(
    private val rankingEventService: RankingEventService,
    private val objectMapper: ObjectMapper,
    private val orderJpaRepository: OrderJpaRepository,
    private val failedEventService: FailedEventService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["catalog-events", "order-events"],
        groupId = "commerce-streamer-ranking",
        containerFactory = KafkaConfig.SINGLE_LISTENER,
    )
    fun handleRankingEvent(
        record: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            val eventId = record.headers().lastHeader("outbox-event-id")
                ?.value()?.let { String(it) } ?: return
            val eventType = record.headers().lastHeader("outbox-event-type")
                ?.value()?.let { String(it) } ?: return

            log.debug("[RankingEvent] type={} key={} eventId={}", eventType, record.key(), eventId)

            when (eventType) {
                "ProductViewedBatch" -> handleViewBatch(record, eventId)
                "ProductLiked" -> handleLike(record, eventId)
                "OrderCreated" -> handleOrder(record, eventId)
                else -> { /* skip non-ranking events */ }
            }
        } catch (e: Exception) {
            log.error("[RankingEvent] Failed to process message: offset={}", record.offset(), e)
            failedEventService.save(record, e)
        } finally {
            acknowledgment.acknowledge()
        }
    }

    private fun handleViewBatch(record: ConsumerRecord<String, String>, eventId: String) {
        val payload = objectMapper.readTree(record.value())
        val views = payload.path("views").map {
            ViewCount(
                productId = it.path("productId").asLong(),
                count = it.path("count").asLong(),
            )
        }
        rankingEventService.saveViewBatch(views, eventId)
    }

    private fun handleLike(record: ConsumerRecord<String, String>, eventId: String) {
        val productId = record.key().toLong()
        rankingEventService.saveLikeEvent(productId, eventId.toLong())
    }

    private fun handleOrder(record: ConsumerRecord<String, String>, eventId: String) {
        val payload = objectMapper.readTree(record.value())
        val orderId = payload.path("orderId").asLong()
        val order = orderJpaRepository.findById(orderId).orElse(null)
        if (order != null) {
            val items = order.items.map {
                OrderItemMetrics(
                    productId = it.productId,
                    productPrice = it.productPrice,
                    quantity = it.quantity,
                )
            }
            rankingEventService.saveOrderEvent(items, eventId.toLong())
        } else {
            log.warn("[RankingEvent] Order not found: orderId={}", orderId)
        }
    }
}
