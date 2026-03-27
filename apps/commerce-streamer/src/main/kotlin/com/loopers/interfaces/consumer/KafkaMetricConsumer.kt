package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.metric.KafkaEventEnvelope
import com.loopers.application.metric.KafkaMetricEventHandler
import com.loopers.config.kafka.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class KafkaMetricConsumer(
    private val objectMapper: ObjectMapper,
    private val kafkaMetricEventHandler: KafkaMetricEventHandler,
) {
    @KafkaListener(
        topics = ["catalog-events", "order-events"],
        containerFactory = KafkaConfig.BATCH_LISTENER,
    )
    fun consume(
        records: List<ConsumerRecord<String, ByteArray>>,
        acknowledgment: Acknowledgment,
    ) {
        records.forEach { record ->
            val envelope = objectMapper.readValue(record.value(), KafkaEventEnvelope::class.java)
            kafkaMetricEventHandler.handle(record.topic(), envelope)
        }
        acknowledgment.acknowledge()
    }
}
