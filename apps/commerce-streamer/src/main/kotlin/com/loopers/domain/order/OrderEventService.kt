package com.loopers.domain.order

import com.loopers.config.kafka.message.OrderMessage
import com.loopers.domain.event.EventHandledModel
import com.loopers.domain.event.EventHandledRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderEventService(
    private val eventHandledRepository: EventHandledRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun handleOrderCreated(message: OrderMessage) {
        if (eventHandledRepository.existsByEventId(message.eventId)) {
            log.info("이미 처리된 이벤트 - eventId: {}", message.eventId)
            return
        }

        log.info("주문 이벤트 수신 - orderId: {}, userId: {}, totalPrice: {}",
            message.orderId, message.userId, message.totalPrice)

        eventHandledRepository.save(
            EventHandledModel(
                eventId = message.eventId,
                eventType = "ORDER_CREATED",
            ),
        )
    }
}
