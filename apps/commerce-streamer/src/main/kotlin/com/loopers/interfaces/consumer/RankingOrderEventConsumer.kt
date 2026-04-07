package com.loopers.interfaces.consumer

import com.loopers.application.ranking.OrderItemScore
import com.loopers.application.ranking.RankingAggregationService
import com.loopers.config.kafka.KafkaConfig
import com.loopers.event.KafkaEventMessage
import com.loopers.event.KafkaTopics
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

@Component
class RankingOrderEventConsumer(
    private val rankingAggregationService: RankingAggregationService,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopics.ORDER_EVENTS],
        groupId = RankingCatalogEventConsumer.CONSUMER_GROUP,
        containerFactory = KafkaConfig.BATCH_LISTENER,
    )
    fun consume(messages: List<ConsumerRecord<Any, Any>>, acknowledgment: Acknowledgment) {
        for (record in messages) {
            try {
                val message = objectMapper.readValue(record.value() as ByteArray, KafkaEventMessage::class.java)
                val date = message.occurredAt.toLocalDate()
                val dateTime = message.occurredAt.toLocalDateTime()

                when (message.eventType) {
                    "ORDER_CREATED" -> handleOrderCreated(message, date, dateTime)
                }
            } catch (e: Exception) {
                log.error("랭킹 order 이벤트 처리 실패: record offset=${record.offset()}", e)
            }
        }
        acknowledgment.acknowledge()
    }

    private fun handleOrderCreated(message: KafkaEventMessage, date: LocalDate, dateTime: java.time.LocalDateTime) {
        val items = extractOrderItems(message.payload)
        rankingAggregationService.processOrderEvent(items, date, dateTime)
    }

    private fun extractOrderItems(payload: Map<String, Any?>): List<OrderItemScore> {
        val items = payload["items"] as? List<*> ?: return emptyList()
        return items.mapNotNull { item ->
            if (item is Map<*, *>) {
                val productId = (item["productId"] as? Number)?.toLong() ?: return@mapNotNull null
                val unitPrice = toBigDecimal(item["unitPrice"]) ?: return@mapNotNull null
                val quantity = (item["quantity"] as? Number)?.toInt() ?: return@mapNotNull null
                OrderItemScore(
                    productId = productId,
                    amount = unitPrice.multiply(BigDecimal(quantity)),
                )
            } else {
                null
            }
        }
    }

    private fun toBigDecimal(value: Any?): BigDecimal? {
        return when (value) {
            is Number -> BigDecimal(value.toString())
            is String -> value.toBigDecimalOrNull()
            else -> null
        }
    }
}
