package com.loopers.application.metric

import com.fasterxml.jackson.databind.JsonNode
import com.loopers.domain.metric.HandledEvent
import com.loopers.domain.metric.HandledEventRepository
import com.loopers.domain.metric.ProductLikeCountRepository
import com.loopers.domain.metric.ProductMetric
import com.loopers.domain.metric.ProductMetricDaily
import com.loopers.domain.metric.ProductMetricDailyRepository
import com.loopers.domain.metric.ProductMetricRepository
import com.loopers.domain.metric.ProcessedPaymentRepository
import com.loopers.domain.ranking.ProductRankingRepository
import com.loopers.domain.ranking.RankingScorePolicy
import com.loopers.domain.ranking.RankingSignalType
import com.loopers.infrastructure.outbox.KafkaEventType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@Service
class KafkaMetricEventHandler(
    private val productMetricRepository: ProductMetricRepository,
    private val productMetricDailyRepository: ProductMetricDailyRepository,
    private val handledEventRepository: HandledEventRepository,
    private val productLikeCountRepository: ProductLikeCountRepository,
    private val processedPaymentRepository: ProcessedPaymentRepository,
    private val productRankingRepository: ProductRankingRepository,
    private val clock: Clock = Clock.system(SEOUL_ZONE),
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

        // Daily 적재 (LIKE_CANCELED는 no-op — 기존 ZSET과 일관: cancel 시 점수 차감 없음)
        val today = LocalDate.now(clock)
        when (envelope.eventType) {
            KafkaEventType.PRODUCT_DETAIL_VIEWED -> {
                val daily = productMetricDailyRepository.findByProductIdAndMetricDate(productId, today)
                    ?: ProductMetricDaily.register(productId, today)
                productMetricDailyRepository.save(daily.recordView())
            }
            KafkaEventType.PRODUCT_LIKE_REGISTERED -> {
                val daily = productMetricDailyRepository.findByProductIdAndMetricDate(productId, today)
                    ?: ProductMetricDaily.register(productId, today)
                productMetricDailyRepository.save(daily.recordLike())
            }
            else -> Unit
        }

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

        val itemsNode = envelope.payload["items"]?.takeIf { it.isArray }

        // 정책 C: raw payload per-item 필수 필드 누락 또는 음수 필드 → 이벤트 전체 skip.
        // mandatory 필드(productId, quantity) 누락 아이템이 per-item skip으로 조용히 사라지면
        // paymentId 멱등성과 결합해 해당 아이템이 영구 유실되므로, 이벤트 전체 격리로 처리.
        // 집계 후(totalQuantity/totalAmount) 검사는 같은 productId 음수+양수 상쇄로 빠져나갈 여지 있음.
        if (itemsNode != null && itemsNode.any { node -> hasInvalidOrderItemFields(node) }) {
            log.warn(
                "Skipping PAYMENT_SUCCEEDED with invalid item fields: eventId={}, paymentId={}",
                envelope.eventId,
                paymentId,
            )
            return
        }

        val parsedItems = itemsNode
            ?.mapNotNull { node ->
                if (node.isNull) return@mapNotNull null

                val productIdNode = node["productId"] ?: return@mapNotNull null
                if (!productIdNode.canConvertToLong()) {
                    log.warn(
                        "Skip item with non-numeric productId. eventId={}, productId={}",
                        envelope.eventId,
                        productIdNode,
                    )
                    return@mapNotNull null
                }
                val productId = productIdNode.asLong()

                val quantityNode = node["quantity"] ?: return@mapNotNull null
                if (!quantityNode.isIntegralNumber) {
                    log.warn(
                        "Skip item with non-integer quantity. eventId={}, productId={}, quantity={}",
                        envelope.eventId,
                        productId,
                        quantityNode,
                    )
                    return@mapNotNull null
                }
                val quantity = quantityNode.asInt()
                if (quantity <= 0) {
                    log.warn(
                        "Skip item with invalid quantity. eventId={}, productId={}, quantity={}",
                        envelope.eventId,
                        productId,
                        quantity,
                    )
                    return@mapNotNull null
                }

                val sellingPriceNode = node["sellingPrice"]
                val sellingPrice = when {
                    sellingPriceNode == null || sellingPriceNode.isNull -> null
                    !sellingPriceNode.canConvertToLong() -> {
                        log.warn(
                            "Skip item with non-numeric sellingPrice. eventId={}, productId={}, sellingPrice={}",
                            envelope.eventId,
                            productId,
                            sellingPriceNode,
                        )
                        return@mapNotNull null
                    }
                    else -> sellingPriceNode.asLong()
                }

                Triple(productId, quantity, sellingPrice)
            }
            .orEmpty()

        if (parsedItems.isEmpty()) {
            log.warn("Skip payment event without valid items. eventId={}", envelope.eventId)
            return
        }

        data class AggregatedItem(val totalQuantity: Int, val totalAmount: Long)

        val itemsByProduct = parsedItems
            .groupBy { it.first }
            .mapValues { (_, items) ->
                AggregatedItem(
                    totalQuantity = items.sumOf { it.second },
                    totalAmount = items.sumOf { (it.third ?: 0L) * it.second },
                )
            }

        val updatedMetrics = itemsByProduct.mapNotNull { (productId, item) ->
            val current = productMetricRepository.findByProductId(productId) ?: ProductMetric.register(productId)
            current.recordUnitsSold(item.totalQuantity)
        }

        if (updatedMetrics.isEmpty()) {
            log.debug("Skip stale payment event. eventId={}, topic={}", envelope.eventId, topic)
            return
        }

        productMetricRepository.saveAll(updatedMetrics)
        processedPaymentRepository.save(paymentId)

        val today = LocalDate.now(clock)
        itemsByProduct.forEach { (productId, item) ->
            val daily = productMetricDailyRepository.findByProductIdAndMetricDate(productId, today)
                ?: ProductMetricDaily.register(productId, today)
            productMetricDailyRepository.save(daily.recordOrder(item.totalQuantity, item.totalAmount))
            updateOrderRankingScore(productId, item.totalAmount, envelope.eventId)
        }
    }

    private fun updateRankingScore(
        productId: Long,
        signalType: RankingSignalType,
        eventId: Long,
    ) {
        runCatching {
            val increment = rankingScorePolicy.calculateIncrement(signalType)
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

    private fun updateOrderRankingScore(
        productId: Long,
        totalAmount: Long,
        eventId: Long,
    ) {
        runCatching {
            val increment = rankingScorePolicy.calculateOrderIncrement(totalAmount)
            if (increment > 0) {
                productRankingRepository.incrementScore(productId, increment)
            }
        }.onFailure { e ->
            log.warn(
                "Failed to update order ranking score. productId={}, eventId={}",
                productId,
                eventId,
                e,
            )
        }
    }

    /**
     * 정책 C — PAYMENT_SUCCEEDED 아이템 노드가 이벤트 전체 skip 대상인지 판단한다.
     *
     * - null 노드는 개별 스킵 대상이므로 false (`handleOrderEvent` 파싱 단계에서 처리).
     * - `productId`, `quantity`는 mandatory: 누락/null 시 전체 skip.
     * - `quantity`가 정수이면서 음수이면 전체 skip. 비정수/문자열은 여기서는 false 반환하고
     *   per-item 파싱 경로에서 WARN + skip 처리 (Jackson 타입 coercion 방어).
     * - `sellingPrice`는 optional이지만, 숫자로 변환 가능하면서 음수이면 전체 skip.
     *   비숫자 문자열은 마찬가지로 per-item 경로에서 처리.
     */
    private fun hasInvalidOrderItemFields(node: JsonNode): Boolean {
        if (node.isNull) return false
        val productIdNode = node["productId"] ?: return true
        val quantityNode = node["quantity"] ?: return true
        if (productIdNode.isNull || quantityNode.isNull) return true
        if (quantityNode.isIntegralNumber && quantityNode.asInt() < 0) return true
        val sellingPriceNode = node["sellingPrice"]
        if (sellingPriceNode != null &&
            !sellingPriceNode.isNull &&
            sellingPriceNode.canConvertToLong() &&
            sellingPriceNode.asLong() < 0
        ) {
            return true
        }
        return false
    }

    companion object {
        private val SEOUL_ZONE = ZoneId.of("Asia/Seoul")
    }
}
