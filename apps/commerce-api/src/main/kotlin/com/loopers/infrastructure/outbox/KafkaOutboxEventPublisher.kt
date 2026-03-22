package com.loopers.infrastructure.outbox

import com.loopers.application.event.OutboxEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class KafkaOutboxEventPublisher(
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
) : OutboxEventPublisher {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun publish(topic: String, key: String, payload: Map<String, Any?>) {
        kafkaTemplate.send(topic, key, payload).get(5, TimeUnit.SECONDS)
        log.debug("Kafka 발행 완료: topic={}, key={}", topic, key)
    }
}
