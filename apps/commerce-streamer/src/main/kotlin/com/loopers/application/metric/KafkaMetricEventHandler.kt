package com.loopers.application.metric

import com.loopers.domain.metric.HandledEvent
import com.loopers.domain.metric.HandledEventRepository
import com.loopers.domain.metric.ProductLikeCountRepository
import com.loopers.domain.metric.ProductMetric
import com.loopers.domain.metric.ProductMetricRepository
import com.loopers.domain.metric.ProcessedPaymentRepository
import com.loopers.infrastructure.outbox.KafkaEventType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KafkaMetricEventHandler(
    private val productMetricRepository: ProductMetricRepository,
    private val handledEventRepository: HandledEventRepository,
    private val productLikeCountRepository: ProductLikeCountRepository,
    private val processedPaymentRepository: ProcessedPaymentRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun handle(topic: String, envelope: KafkaEventEnvelope) {
        if (handledEventRepository.existsByEventId(envelope.eventId)) {
            log.debug(
                "Skip already handled event. eventId={}, topic={}, eventType={}",
                envelope.eventId,
                topic,
                envelope.eventType,
            )
            return
        }

        when (topic) {
            "catalog-events" -> handleCatalogEvent(topic, envelope)
            "order-events" -> handleOrderEvent(topic, envelope)
            else -> log.warn("Skip unknown topic. topic={}, eventId={}", topic, envelope.eventId)
        }

        handledEventRepository.save(
            HandledEvent(
                eventId = envelope.eventId,
                topic = topic,
                eventType = envelope.eventType,
            ),
        )
    }

    private fun handleCatalogEvent(
        topic: String,
        envelope: KafkaEventEnvelope,
    ) {
        val productId = envelope.aggregateId
        val current = productMetricRepository.findByProductId(productId) ?: ProductMetric.register(productId)
        val updated = when (envelope.eventType) {
            KafkaEventType.PRODUCT_DETAIL_VIEWED -> current.recordDetailViewed(envelope.eventId)
            KafkaEventType.PRODUCT_LIKE_REGISTERED,
            KafkaEventType.PRODUCT_LIKE_CANCELED,
            ->
                current.synchronizeLikeCount(
                    eventVersion = envelope.eventId,
                    likeCount = productLikeCountRepository.countByProductId(productId),
                )
            KafkaEventType.PAYMENT_SUCCEEDED -> {
                log.warn("Unexpected payment event on catalog topic. eventId={}", envelope.eventId)
                null
            }
        } ?: run {
            log.debug("Skip stale catalog event. eventId={}, topic={}, aggregateId={}", envelope.eventId, topic, productId)
            return
        }

        productMetricRepository.save(updated)
    }

    private fun handleOrderEvent(
        topic: String,
        envelope: KafkaEventEnvelope,
    ) {
        if (envelope.eventType != KafkaEventType.PAYMENT_SUCCEEDED) {
            log.warn("Skip unsupported order event type. eventId={}, eventType={}", envelope.eventId, envelope.eventType)
            return
        }

        val paymentId = envelope.payload["paymentId"]?.asLong()
        if (paymentId == null) {
            log.warn("Skip payment event without paymentId. eventId={}", envelope.eventId)
            return
        }

        if (processedPaymentRepository.existsByPaymentId(paymentId)) {
            log.debug(
                "Skip already processed payment. paymentId={}, eventId={}, topic={}",
                paymentId,
                envelope.eventId,
                topic,
            )
            return
        }

        val items = envelope.payload["items"]
            ?.takeIf { it.isArray }
            ?.mapNotNull { node -> if (node.isNull) null else node }
            .orEmpty()

        if (items.isEmpty()) {
            log.warn("Skip payment event without items. eventId={}", envelope.eventId)
            return
        }

        val updatedMetrics = items.mapNotNull { item ->
            val productId = item["productId"]?.asLong() ?: return@mapNotNull null
            val quantity = item["quantity"]?.asInt() ?: return@mapNotNull null
            val current = productMetricRepository.findByProductId(productId) ?: ProductMetric.register(productId)
            current.recordUnitsSold(quantity)
        }

        if (updatedMetrics.isEmpty()) {
            log.debug("Skip stale payment event. eventId={}, topic={}", envelope.eventId, topic)
            return
        }

        productMetricRepository.saveAll(updatedMetrics)
        processedPaymentRepository.save(paymentId)
    }
}
