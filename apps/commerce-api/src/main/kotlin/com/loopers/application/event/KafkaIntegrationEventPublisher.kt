package com.loopers.application.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.kafka.IntegrationEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaIntegrationEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    fun publish(topic: String, event: IntegrationEvent<*>) {
        kafkaTemplate.send(topic, event.key, objectMapper.writeValueAsString(event)).get()
    }
}
