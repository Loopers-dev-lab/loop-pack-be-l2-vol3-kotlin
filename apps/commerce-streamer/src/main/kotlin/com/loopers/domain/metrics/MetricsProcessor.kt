package com.loopers.domain.metrics

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.event.EventHandled
import com.loopers.domain.event.EventHandledRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

/**
 * 메트릭 집계 처리 서비스.
 *
 * MetricsConsumer에서 호출되며, 건별 트랜잭션(REQUIRES_NEW)을 담당한다.
 * 한 건이 실패해도 다른 건의 트랜잭션에 영향을 주지 않는다.
 */
@Component
class MetricsProcessor(
    private val productMetricsRepository: ProductMetricsRepository,
    private val eventHandledRepository: EventHandledRepository,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private val log = LoggerFactory.getLogger(MetricsProcessor::class.java)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun process(record: ConsumerRecord<String, String>) {
        val payload = objectMapper.readTree(record.value())
        val eventId = payload.get("eventId")?.asText() ?: return
        val eventType = payload.get("eventType")?.asText() ?: return
        val occurredAt = payload.get("occurredAt")?.asText()?.let { ZonedDateTime.parse(it) }

        // 멱등 체크
        if (eventHandledRepository.existsByEventId(eventId)) {
            log.debug("이미 처리된 이벤트 skip [eventId={}]", eventId)
            return
        }

        // 이벤트 타입별 처리
        when (eventType) {
            "PRODUCT_LIKED" -> handleMetricsUpdate(payload, occurredAt) { it.incrementLikeCount() }
            "PRODUCT_UNLIKED" -> handleMetricsUpdate(payload, occurredAt) { it.decrementLikeCount() }
            "ORDER_CREATED" -> handleOrderCreated(payload, occurredAt)
            "PRODUCT_VIEWED" -> handleMetricsUpdate(payload, occurredAt) { it.incrementViewCount() }
            else -> log.debug("미처리 이벤트 타입 [type={}]", eventType)
        }

        // 처리 완료 기록
        eventHandledRepository.save(EventHandled(eventId = eventId))
    }

    /**
     * 공통 메트릭 업데이트.
     * stale 이벤트 체크 후 증감 로직을 실행한다.
     */
    private fun handleMetricsUpdate(
        payload: JsonNode,
        occurredAt: ZonedDateTime?,
        action: (ProductMetrics) -> Unit,
    ) {
        val productId = payload.get("aggregateId")?.asLong() ?: return
        val metrics = getOrCreateMetrics(productId)

        if (occurredAt != null && metrics.isStaleEvent(occurredAt)) {
            log.debug("[메트릭] stale 이벤트 skip [productId={}, occurredAt={}]", productId, occurredAt)
            return
        }

        action(metrics)
        occurredAt?.let { metrics.updateLastEventAt(it) }
        productMetricsRepository.save(metrics)
        log.info("[메트릭] 업데이트 [productId={}, type={}]", productId, payload.get("eventType")?.asText())
    }

    private fun handleOrderCreated(payload: JsonNode, occurredAt: ZonedDateTime?) {
        val orderItems = payload.get("orderItems") ?: return
        orderItems.forEach { item ->
            val productId = item.get("productId")?.asLong() ?: return@forEach
            val quantity = item.get("quantity")?.asInt() ?: 1
            val metrics = getOrCreateMetrics(productId)

            if (occurredAt != null && metrics.isStaleEvent(occurredAt)) {
                log.debug("[메트릭] stale 이벤트 skip [productId={}, occurredAt={}]", productId, occurredAt)
                return@forEach
            }

            metrics.incrementOrderCount(quantity.toLong())
            occurredAt?.let { metrics.updateLastEventAt(it) }
            productMetricsRepository.save(metrics)
            log.info("[메트릭] 주문 집계 [productId={}, quantity={}]", productId, quantity)
        }
    }

    private fun getOrCreateMetrics(productId: Long): ProductMetrics {
        return productMetricsRepository.findByProductId(productId)
            ?: ProductMetrics(productId = productId)
    }
}
