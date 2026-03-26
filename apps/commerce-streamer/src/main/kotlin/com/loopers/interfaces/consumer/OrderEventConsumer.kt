package com.loopers.interfaces.consumer

import com.loopers.application.event.IdempotencyService
import com.loopers.application.metrics.MetricsAggregationService
import com.loopers.config.kafka.KafkaConfig
import com.loopers.event.KafkaEventMessage
import com.loopers.event.KafkaTopics
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class OrderEventConsumer(
    private val idempotencyService: IdempotencyService,
    private val metricsAggregationService: MetricsAggregationService,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopics.ORDER_EVENTS],
        groupId = "metrics-consumer",
        containerFactory = KafkaConfig.BATCH_LISTENER,
    )
    fun consume(messages: List<ConsumerRecord<Any, Any>>, acknowledgment: Acknowledgment) {
        for (record in messages) {
            try {
                val message = objectMapper.convertValue(record.value(), KafkaEventMessage::class.java)

                if (idempotencyService.isAlreadyHandled(message.eventId)) {
                    continue
                }

                when (message.eventType) {
                    "ORDER_CREATED" -> handleOrderCreated(message)
                    "PAYMENT_COMPLETED" -> log.info("결제 완료 이벤트 수신: orderId=${message.aggregateId}")
                    "PAYMENT_FAILED" -> log.info("결제 실패 이벤트 수신: orderId=${message.aggregateId}")
                    else -> log.warn("알 수 없는 order 이벤트 타입: ${message.eventType}")
                }

                idempotencyService.markHandled(
                    eventId = message.eventId,
                    aggregateType = message.aggregateType,
                    aggregateId = message.aggregateId,
                    eventType = message.eventType,
                )
            } catch (e: Exception) {
                log.error("order 이벤트 처리 실패: record offset=${record.offset()}", e)
            }
        }
        acknowledgment.acknowledge()
    }

    private fun handleOrderCreated(message: KafkaEventMessage) {
        val productIds = message.payload["productIds"]
        if (productIds is List<*>) {
            for (productId in productIds) {
                val id = when (productId) {
                    is Number -> productId.toLong()
                    else -> continue
                }
                metricsAggregationService.incrementOrderCount(id, message.version)
            }
        }
    }
}
