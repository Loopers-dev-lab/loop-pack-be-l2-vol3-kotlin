package com.loopers.interfaces.consumer

import com.loopers.config.kafka.KafkaConfig
import com.loopers.config.kafka.message.OrderMessage
import com.loopers.domain.order.OrderEventService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class OrderEventKafkaConsumer(
    private val orderEventService: OrderEventService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["order.created.v1"],
        groupId = "commerce-streamer-order",
        containerFactory = KafkaConfig.SINGLE_LISTENER,
    )
    fun consume(message: OrderMessage, acknowledgment: Acknowledgment) {
        try {
            orderEventService.handleOrderCreated(message)
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            log.error("주문 이벤트 처리 실패 - eventId: {}", message.eventId, e)
        }
    }
}
