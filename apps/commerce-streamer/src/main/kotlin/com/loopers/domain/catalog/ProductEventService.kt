package com.loopers.domain.catalog

import com.loopers.config.kafka.message.ProductLikedMessage
import com.loopers.config.kafka.message.ProductViewedMessage
import com.loopers.domain.event.EventHandledModel
import com.loopers.domain.event.EventHandledRepository
import com.loopers.infrastructure.catalog.ProductRankRedisRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductEventService(
    private val eventHandledRepository: EventHandledRepository,
    private val productRankRedisRepository: ProductRankRedisRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun handleProductViewed(message: ProductViewedMessage) {
        if (eventHandledRepository.existsByEventId(message.eventId)) {
            log.info("이미 처리된 이벤트 - eventId: {}", message.eventId)
            return
        }

        log.info("상품 조회 이벤트 수신 - productId: {}, userId: {}", message.productId, message.userId)

        eventHandledRepository.save(
            EventHandledModel(
                eventId = message.eventId,
                eventType = "PRODUCT_VIEWED",
            ),
        )

        productRankRedisRepository.incrementView(message.productId, message.occurredAt.toLocalDate())
    }

    @Transactional
    fun handleProductLiked(message: ProductLikedMessage) {
        if (eventHandledRepository.existsByEventId(message.eventId)) {
            log.info("이미 처리된 이벤트 - eventId: {}", message.eventId)
            return
        }

        log.info("상품 좋아요 이벤트 수신 - productId: {}, userId: {}", message.productId, message.userId)

        eventHandledRepository.save(
            EventHandledModel(
                eventId = message.eventId,
                eventType = "PRODUCT_LIKED",
            ),
        )

        productRankRedisRepository.incrementLike(message.productId, message.occurredAt.toLocalDate())
    }
}
