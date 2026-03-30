package com.loopers.interfaces.consumer

import com.loopers.application.CatalogEventProcessor
import com.loopers.event.EventEnvelope
import com.loopers.event.Topics
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class CatalogEventConsumer(
    private val catalogEventProcessor: CatalogEventProcessor,
) {

    @KafkaListener(
        topics = [Topics.CATALOG],
        groupId = "catalog-collector",
        containerFactory = "catalogListenerContainerFactory",
    )
    fun consume(@Payload envelope: EventEnvelope, ack: Acknowledgment) {
        catalogEventProcessor.process(envelope)
        ack.acknowledge()
    }
}
