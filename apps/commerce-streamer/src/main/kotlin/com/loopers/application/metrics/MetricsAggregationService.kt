package com.loopers.application.metrics

import com.loopers.domain.metrics.ProductMetrics
import com.loopers.domain.metrics.ProductMetricsRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MetricsAggregationService(
    private val productMetricsRepository: ProductMetricsRepository,
) {

    @Transactional
    fun incrementLikeCount(productId: Long, eventVersion: Long) {
        val metrics = getOrCreateMetrics(productId)
        if (metrics.isStaleEvent(eventVersion)) return
        metrics.incrementLikeCount(eventVersion)
        productMetricsRepository.save(metrics)
    }

    @Transactional
    fun decrementLikeCount(productId: Long, eventVersion: Long) {
        val metrics = getOrCreateMetrics(productId)
        if (metrics.isStaleEvent(eventVersion)) return
        metrics.decrementLikeCount(eventVersion)
        productMetricsRepository.save(metrics)
    }

    @Transactional
    fun incrementViewCount(productId: Long, eventVersion: Long) {
        val metrics = getOrCreateMetrics(productId)
        if (metrics.isStaleEvent(eventVersion)) return
        metrics.incrementViewCount(eventVersion)
        productMetricsRepository.save(metrics)
    }

    @Transactional
    fun incrementOrderCount(productId: Long, eventVersion: Long) {
        val metrics = getOrCreateMetrics(productId)
        if (metrics.isStaleEvent(eventVersion)) return
        metrics.incrementOrderCount(eventVersion)
        productMetricsRepository.save(metrics)
    }

    private fun getOrCreateMetrics(productId: Long): ProductMetrics {
        return productMetricsRepository.findByProductId(productId)
            ?: productMetricsRepository.save(ProductMetrics(productId = productId))
    }
}
