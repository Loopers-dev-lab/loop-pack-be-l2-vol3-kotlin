package com.loopers.application.consumer

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class DeadLetterPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    private val log = LoggerFactory.getLogger(DeadLetterPublisher::class.java)

    fun publish(sourceTopic: String, key: String?, payload: String, cause: Throwable) {
        val dlqTopic = "$sourceTopic.dlq"
        log.warn("publish dead letter sourceTopic={} dlqTopic={} key={}", sourceTopic, dlqTopic, key, cause)
        if (key == null) {
            kafkaTemplate.send(dlqTopic, payload)
            return
        }
        kafkaTemplate.send(dlqTopic, key, payload)
    }
}
