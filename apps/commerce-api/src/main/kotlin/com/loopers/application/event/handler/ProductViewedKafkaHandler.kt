package com.loopers.application.event.handler

import com.loopers.application.event.ProductViewedEvent
import com.loopers.event.KafkaEventMessage
import com.loopers.event.KafkaTopics
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.util.UUID

@Component
class ProductViewedKafkaHandler(
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async("asyncEventExecutor")
    @EventListener
    fun handle(event: ProductViewedEvent) {
        try {
            val message = KafkaEventMessage(
                eventId = UUID.randomUUID().toString(),
                eventType = "PRODUCT_VIEWED",
                aggregateType = "PRODUCT",
                aggregateId = event.productId.toString(),
                payload = mapOf(
                    "userId" to event.userId,
                    "productId" to event.productId,
                    "loginId" to event.loginId,
                    "clientIp" to event.clientIp,
                    "userAgent" to event.userAgent,
                    "referer" to event.referer,
                ),
                version = System.currentTimeMillis(),
                occurredAt = ZonedDateTime.now(),
            )
            kafkaTemplate.send(KafkaTopics.CATALOG_EVENTS, event.productId.toString(), message)
        } catch (e: Exception) {
            log.warn("ProductViewedEvent Kafka 발행 실패: productId=${event.productId}", e)
        }
    }
}
