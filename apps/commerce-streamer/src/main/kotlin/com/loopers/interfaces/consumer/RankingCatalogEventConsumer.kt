package com.loopers.interfaces.consumer

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

@Component
class RankingCatalogEventConsumer(
    private val rankingAggregationService: RankingAggregationService,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopics.CATALOG_EVENTS],
        groupId = CONSUMER_GROUP,
        containerFactory = KafkaConfig.BATCH_LISTENER,
    )
    fun consume(messages: List<ConsumerRecord<Any, Any>>, acknowledgment: Acknowledgment) {
        for (record in messages) {
            try {
                val message = objectMapper.readValue(record.value() as ByteArray, KafkaEventMessage::class.java)
                val productId = message.aggregateId.toLong()
                val date = message.occurredAt.toLocalDate()
                val dateTime = message.occurredAt.toLocalDateTime()

                when (message.eventType) {
                    "PRODUCT_VIEWED" -> rankingAggregationService.processViewEvent(productId, date, dateTime)
                    "PRODUCT_LIKED" -> rankingAggregationService.processLikeEvent(productId, date, dateTime)
                }
            } catch (e: Exception) {
                log.error("랭킹 catalog 이벤트 처리 실패: record offset=${record.offset()}", e)
            }
        }
        acknowledgment.acknowledge()
    }

    companion object {
        const val CONSUMER_GROUP = "ranking-consumer"
    }
}
