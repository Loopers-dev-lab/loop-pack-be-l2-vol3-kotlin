package com.loopers.interfaces.consumer

import com.loopers.application.metrics.ProductMetricsEventHandler
import com.loopers.config.kafka.KafkaConfig
import com.loopers.config.kafka.event.CatalogEventMessage
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class DemoKafkaConsumer(
    private val productMetricsEventHandler: ProductMetricsEventHandler,
) {
    @KafkaListener(
        topics = ["\${step2.kafka.catalog-topic}"],
        groupId = "\${step2.kafka.catalog-consumer-group}",
        containerFactory = KafkaConfig.BATCH_LISTENER,
    )
    fun catalogMetricsListener(
        messages: List<ConsumerRecord<String, CatalogEventMessage>>,
        acknowledgment: Acknowledgment,
    ) {
        messages.forEach { message ->
            productMetricsEventHandler.handle(message.value())
        }
        acknowledgment.acknowledge()
    }
}
