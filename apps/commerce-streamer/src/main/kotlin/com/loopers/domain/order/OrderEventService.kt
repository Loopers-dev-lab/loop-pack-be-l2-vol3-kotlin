package com.loopers.domain.order

import com.loopers.config.kafka.message.OrderMessage
import com.loopers.domain.event.EventHandledModel
import com.loopers.domain.event.EventHandledRepository
import com.loopers.infrastructure.catalog.ProductRankRedisRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class OrderEventService(
    private val eventHandledRepository: EventHandledRepository,
    private val productRankRedisRepository: ProductRankRedisRepository,
    private val transactionTemplate: TransactionTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun handleOrderCreated(message: OrderMessage) {
        val processed = transactionTemplate.execute {
            if (eventHandledRepository.existsByEventId(message.eventId)) {
                log.info("이미 처리된 이벤트 - eventId: {}", message.eventId)
                return@execute false
            }

            log.info("주문 이벤트 수신 - orderId: {}, userId: {}, totalPrice: {}",
                message.orderId, message.userId, message.totalPrice)

            eventHandledRepository.save(
                EventHandledModel(
                    eventId = message.eventId,
                    eventType = "ORDER_CREATED",
                ),
            )
            true
        } ?: false

        // DB 커밋 이후에 Redis 반영 — 롤백 시 이중 가산/부분 반영 방지
        if (processed) {
            val date = message.occurredAt.toLocalDate()
            message.items.forEach { item ->
                productRankRedisRepository.incrementOrder(item.productId, item.quantity.toLong(), date)
            }
        }
    }
}
