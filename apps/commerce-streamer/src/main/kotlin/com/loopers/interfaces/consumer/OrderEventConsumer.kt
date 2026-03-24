package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.OrderEventProcessor
import com.loopers.event.EventEnvelope
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class OrderEventConsumer(
    private val orderEventProcessor: OrderEventProcessor,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["order-events"],
        groupId = "order-collector",
    )
    fun consume(record: ConsumerRecord<String, String>, ack: Acknowledgment) {
        try {
            val envelope = objectMapper.readValue(record.value(), EventEnvelope::class.java)
            orderEventProcessor.process(envelope)
            ack.acknowledge()
        } catch (e: Exception) {
            log.error("[OrderConsumer] 처리 실패: key={}, error={}", record.key(), e.message, e)
            throw e
        }
    }
}
