package com.loopers.application.metric

import com.loopers.domain.metric.HandledEvent
import com.loopers.domain.metric.HandledEventRepository
import com.loopers.domain.metric.ProductLikeCountRepository
import com.loopers.domain.metric.ProductMetric
import com.loopers.domain.metric.ProductMetricRepository
import com.loopers.domain.metric.ProcessedPaymentRepository
import com.loopers.domain.ranking.ProductRankingRepository
import com.loopers.domain.ranking.RankingScorePolicy
import com.loopers.domain.ranking.RankingSignalType
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
    private val productRankingRepository: ProductRankingRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val rankingScorePolicy = RankingScorePolicy()

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
            KafkaEventType.COUPON_ISSUE_REQUESTED -> {
                log.warn("Unexpected coupon event on catalog topic. eventId={}", envelope.eventId)
                null
            }
        } ?: run {
            log.debug("Skip stale catalog event. eventId={}, topic={}, aggregateId={}", envelope.eventId, topic, productId)
            return
        }

        productMetricRepository.save(updated)

        val signalType = when (envelope.eventType) {
            KafkaEventType.PRODUCT_DETAIL_VIEWED -> RankingSignalType.VIEW
            KafkaEventType.PRODUCT_LIKE_REGISTERED -> RankingSignalType.LIKE
            else -> null
        }
        if (signalType != null) {
            updateRankingScore(productId, signalType, envelope.eventId)
        }
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

        val parsedItems = envelope.payload["items"]
            ?.takeIf { it.isArray }
            ?.mapNotNull { node ->
                if (node.isNull) return@mapNotNull null
                val productId = node["productId"]?.asLong() ?: return@mapNotNull null
                val quantity = node["quantity"]?.asInt() ?: return@mapNotNull null
                if (quantity <= 0) {
                    log.warn(
                        "Skip item with invalid quantity. eventId={}, productId={}, quantity={}",
                        envelope.eventId,
                        productId,
                        quantity,
                    )
                    return@mapNotNull null
                }
                productId to quantity
            }
            .orEmpty()

        if (parsedItems.isEmpty()) {
            log.warn("Skip payment event without valid items. eventId={}", envelope.eventId)
            return
        }

        val itemsByProduct = parsedItems
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, quantities) -> quantities.sum() }

        val updatedMetrics = itemsByProduct.mapNotNull { (productId, totalQuantity) ->
            val current = productMetricRepository.findByProductId(productId) ?: ProductMetric.register(productId)
            current.recordUnitsSold(totalQuantity)
        }

        if (updatedMetrics.isEmpty()) {
            log.debug("Skip stale payment event. eventId={}, topic={}", envelope.eventId, topic)
            return
        }

        productMetricRepository.saveAll(updatedMetrics)
        processedPaymentRepository.save(paymentId)

        itemsByProduct.forEach { (productId, totalQuantity) ->
            updateRankingScore(productId, RankingSignalType.ORDER, envelope.eventId, totalQuantity)
        }
    }

    private fun updateRankingScore(
        productId: Long,
        signalType: RankingSignalType,
        eventId: Long,
        quantity: Int = 1,
    ) {
        runCatching {
            val increment = rankingScorePolicy.calculateIncrement(signalType, quantity)
            productRankingRepository.incrementScore(productId, increment)
        }.onFailure { e ->
            log.warn(
                "Failed to update ranking score. productId={}, signalType={}, eventId={}",
                productId,
                signalType,
                eventId,
                e,
            )
        }
    }
}
