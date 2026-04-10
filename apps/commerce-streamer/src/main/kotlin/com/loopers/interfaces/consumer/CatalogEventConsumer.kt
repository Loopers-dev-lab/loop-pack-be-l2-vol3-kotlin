package com.loopers.interfaces.consumer

import com.loopers.config.kafka.KafkaConfig
import com.loopers.domain.deadletter.FailedEventService
import com.loopers.domain.metrics.MetricsService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class CatalogEventConsumer(
    private val metricsService: MetricsService,
    private val failedEventService: FailedEventService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["catalog-events"],
        groupId = "commerce-streamer-catalog",
        containerFactory = KafkaConfig.SINGLE_LISTENER,
    )
    fun handleCatalogEvent(
        record: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            val eventId = record.headers().lastHeader("outbox-event-id")
                ?.value()?.let { String(it).toLong() } ?: return
            val eventType = record.headers().lastHeader("outbox-event-type")
                ?.value()?.let { String(it) } ?: return

            when (eventType) {
                "ProductLiked" -> {
                    val productId = record.key().toLong()
                    log.info("[CatalogEvent] type={} productId={} eventId={}", eventType, productId, eventId)
                    metricsService.incrementLikeCount(productId, eventId)
                }
                "ProductUnliked" -> {
                    val productId = record.key().toLong()
                    log.info("[CatalogEvent] type={} productId={} eventId={}", eventType, productId, eventId)
                    metricsService.decrementLikeCount(productId, eventId)
                }
                else -> log.debug("[CatalogEvent] Skipping event type: {}", eventType)
            }
        } catch (e: Exception) {
            log.error("[CatalogEvent] Failed to process message: offset={}", record.offset(), e)
            failedEventService.save(record, e)
        } finally {
            acknowledgment.acknowledge()
        }
    }
}
