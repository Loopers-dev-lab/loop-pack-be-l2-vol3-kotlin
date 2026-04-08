package com.loopers.interfaces.consumer

import com.loopers.config.kafka.KafkaConfig
import com.loopers.config.kafka.message.ProductLikedMessage
import com.loopers.domain.catalog.ProductEventService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class ProductLikedKafkaConsumer(
    private val productEventService: ProductEventService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["product.liked.v1"],
        groupId = "commerce-streamer-product",
        containerFactory = KafkaConfig.SINGLE_LISTENER,
    )
    fun consume(message: ProductLikedMessage, acknowledgment: Acknowledgment) {
        try {
            productEventService.handleProductLiked(message)
        } catch (e: Exception) {
            log.error("상품 좋아요 이벤트 처리 실패, 건너뜀 - eventId: {}", message.eventId, e)
        } finally {
            acknowledgment.acknowledge()
        }
    }
}
