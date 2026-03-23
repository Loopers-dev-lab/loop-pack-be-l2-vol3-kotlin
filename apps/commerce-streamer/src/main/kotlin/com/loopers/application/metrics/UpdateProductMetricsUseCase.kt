package com.loopers.application.metrics

import com.loopers.domain.event.model.EventHandled
import com.loopers.domain.event.repository.EventHandledRepository
import com.loopers.domain.metrics.model.ProductMetrics
import com.loopers.domain.metrics.repository.ProductMetricsRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpdateProductMetricsUseCase(
    private val productMetricsRepository: ProductMetricsRepository,
    private val eventHandledRepository: EventHandledRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun handleCatalogEvent(eventId: String, eventType: String, productId: Long) {
        if (eventHandledRepository.existsByEventId(eventId)) {
            log.debug("이미 처리된 이벤트: eventId={}", eventId)
            return
        }

        val metrics = findOrCreate(productId)

        when (eventType) {
            PRODUCT_VIEWED -> metrics.incrementViewCount()
            LIKE_ADDED -> metrics.incrementLikeCount()
            LIKE_REMOVED -> metrics.decrementLikeCount()
            else -> {
                log.warn("알 수 없는 catalog 이벤트 타입: eventType={}", eventType)
                eventHandledRepository.save(EventHandled(eventId = eventId))
                return
            }
        }

        productMetricsRepository.save(metrics)
        eventHandledRepository.save(EventHandled(eventId = eventId))
    }

    @Transactional
    fun handleOrderEvent(eventId: String, eventType: String, productId: Long, quantity: Long) {
        if (eventHandledRepository.existsByEventId(eventId)) {
            log.debug("이미 처리된 이벤트: eventId={}", eventId)
            return
        }

        if (eventType != PAYMENT_COMPLETED) {
            log.warn("알 수 없는 order 이벤트 타입: eventType={}", eventType)
            eventHandledRepository.save(EventHandled(eventId = eventId))
            return
        }

        val metrics = findOrCreate(productId)
        metrics.incrementSalesCount(quantity)

        productMetricsRepository.save(metrics)
        eventHandledRepository.save(EventHandled(eventId = eventId))
    }

    private fun findOrCreate(productId: Long): ProductMetrics {
        return productMetricsRepository.findByProductId(productId)
            ?: ProductMetrics(productId = productId)
    }

    companion object {
        const val PRODUCT_VIEWED = "PRODUCT_VIEWED"
        const val LIKE_ADDED = "LIKE_ADDED"
        const val LIKE_REMOVED = "LIKE_REMOVED"
        const val PAYMENT_COMPLETED = "PAYMENT_COMPLETED"
    }
}
