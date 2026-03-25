package com.loopers.interfaces.consumer

import com.loopers.application.OrderEventProcessor
import com.loopers.event.EventEnvelope
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class OrderEventConsumer(
    private val orderEventProcessor: OrderEventProcessor,
) {

    @KafkaListener(
        topics = ["order-events"],
        groupId = "order-collector",
        containerFactory = "orderListenerContainerFactory",
    )
    fun consume(@Payload envelope: EventEnvelope, ack: Acknowledgment) {
        orderEventProcessor.process(envelope)
        ack.acknowledge()
    }
}
