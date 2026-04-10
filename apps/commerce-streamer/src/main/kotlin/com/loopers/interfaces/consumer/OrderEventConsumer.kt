package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.config.kafka.KafkaConfig
import com.loopers.domain.deadletter.FailedEventService
import com.loopers.domain.metrics.MetricsService
import com.loopers.domain.metrics.OrderItemMetrics
import com.loopers.infrastructure.order.OrderJpaRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class OrderEventConsumer(
    private val metricsService: MetricsService,
    private val objectMapper: ObjectMapper,
    private val orderJpaRepository: OrderJpaRepository,
    private val failedEventService: FailedEventService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["order-events"],
        groupId = "commerce-streamer-order",
        containerFactory = KafkaConfig.SINGLE_LISTENER,
    )
    fun handleOrderEvent(
        record: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            val eventId = record.headers().lastHeader("outbox-event-id")
                ?.value()?.let { String(it).toLong() } ?: return
            val eventType = record.headers().lastHeader("outbox-event-type")
                ?.value()?.let { String(it) } ?: return

            log.info("[OrderEvent] type={} orderId={} eventId={}", eventType, record.key(), eventId)

            val payload = objectMapper.readTree(record.value())

            when (eventType) {
                "OrderCreated" -> {
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
                        metricsService.recordOrder(items, eventId)
                    } else {
                        log.warn("[OrderEvent] Order not found: orderId={}", orderId)
                    }
                }
                "PaymentCompleted" -> {
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
                        metricsService.recordPayment(items, eventId)
                    } else {
                        log.warn("[OrderEvent] Order not found: orderId={}", orderId)
                    }
                }
                else -> log.debug("[OrderEvent] Unknown event type: {}", eventType)
            }
        } catch (e: Exception) {
            log.error("[OrderEvent] Failed to process message: offset={}", record.offset(), e)
            failedEventService.save(record, e)
        } finally {
            acknowledgment.acknowledge()
        }
    }
}
