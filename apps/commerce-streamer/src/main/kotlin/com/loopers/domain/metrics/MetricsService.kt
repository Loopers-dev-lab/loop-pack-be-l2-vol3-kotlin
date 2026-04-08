package com.loopers.domain.metrics

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

data class OrderItemMetrics(
    val productId: Long,
    val productPrice: Long,
    val quantity: Int,
)

@Component
class MetricsService(
    private val productMetricsRepository: ProductMetricsRepository,
    private val eventHandledRepository: EventHandledRepository,
) {
    fun isAlreadyHandled(eventId: Long): Boolean {
        return eventHandledRepository.existsByEventId(eventId)
    }

    @Transactional
    fun markHandled(eventId: Long) {
        eventHandledRepository.save(EventHandledRecord(eventId = eventId))
    }

    @Transactional
    fun incrementLikeCount(productId: Long, eventId: Long) {
        if (isAlreadyHandled(eventId)) return
        val metrics = getOrCreateMetrics(productId)
        metrics.incrementLikeCount()
        markHandled(eventId)
    }

    @Transactional
    fun decrementLikeCount(productId: Long, eventId: Long) {
        if (isAlreadyHandled(eventId)) return
        val metrics = getOrCreateMetrics(productId)
        metrics.decrementLikeCount()
        markHandled(eventId)
    }

    @Transactional
    fun recordOrder(items: List<OrderItemMetrics>, eventId: Long) {
        if (isAlreadyHandled(eventId)) return
        items.forEach { item ->
            val metrics = getOrCreateMetrics(item.productId)
            metrics.addOrderCount(item.quantity.toLong())
        }
        markHandled(eventId)
    }

    @Transactional
    fun recordPayment(items: List<OrderItemMetrics>, eventId: Long) {
        if (isAlreadyHandled(eventId)) return
        items.forEach { item ->
            val metrics = getOrCreateMetrics(item.productId)
            metrics.addSales(item.quantity.toLong(), item.productPrice * item.quantity)
        }
        markHandled(eventId)
    }

    private fun getOrCreateMetrics(productId: Long): ProductMetrics {
        return productMetricsRepository.findByProductId(productId)
            ?: productMetricsRepository.save(ProductMetrics(productId = productId))
    }
}
