package com.loopers.application.ranking

import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ViewEventPublisher(
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun publish(productId: Long) {
        try {
            val record = ProducerRecord<Any, Any>(
                TOPIC,
                productId.toString(),
                """{"productId":$productId}""",
            )
            record.headers().add("eventId", UUID.randomUUID().toString().toByteArray())
            record.headers().add("eventType", EVENT_TYPE.toByteArray())
            kafkaTemplate.send(record)
        } catch (e: Exception) {
            log.warn("VIEW 이벤트 발행 실패 [productId={}]", productId, e)
        }
    }

    companion object {
        private const val TOPIC = "catalog-events"
        private const val EVENT_TYPE = "PRODUCT_VIEWED"
    }
}
