package com.loopers.interfaces.consumer

import com.loopers.config.kafka.KafkaConfig
import com.loopers.config.kafka.message.ProductViewedMessage
import com.loopers.domain.catalog.ProductEventService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class ProductViewedKafkaConsumer(
    private val productEventService: ProductEventService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["product.viewed.v1"],
        groupId = "commerce-streamer-product",
        containerFactory = KafkaConfig.SINGLE_LISTENER,
    )
    fun consume(message: ProductViewedMessage, acknowledgment: Acknowledgment) {
        try {
            productEventService.handleProductViewed(message)
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            log.error("상품 조회 이벤트 처리 실패 - eventId: {}", message.eventId, e)
        }
    }
}
