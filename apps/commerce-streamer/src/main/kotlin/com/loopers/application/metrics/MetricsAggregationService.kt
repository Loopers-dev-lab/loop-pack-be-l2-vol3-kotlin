package com.loopers.application.metrics

import com.loopers.domain.metrics.ProductMetrics
import com.loopers.domain.metrics.ProductMetricsRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MetricsAggregationService(
    private val productMetricsRepository: ProductMetricsRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun incrementLikeCount(productId: Long, eventVersion: Long) {
        ensureMetricsExists(productId)
        productMetricsRepository.incrementLikeCount(productId, eventVersion)
    }

    @Transactional
    fun decrementLikeCount(productId: Long, eventVersion: Long) {
        ensureMetricsExists(productId)
        productMetricsRepository.decrementLikeCount(productId, eventVersion)
    }

    @Transactional
    fun incrementViewCount(productId: Long, eventVersion: Long) {
        ensureMetricsExists(productId)
        productMetricsRepository.incrementViewCount(productId, eventVersion)
    }

    @Transactional
    fun incrementOrderCount(productId: Long, eventVersion: Long) {
        ensureMetricsExists(productId)
        productMetricsRepository.incrementOrderCount(productId, eventVersion)
    }

    private fun ensureMetricsExists(productId: Long) {
        if (productMetricsRepository.findByProductId(productId) != null) return
        try {
            productMetricsRepository.save(ProductMetrics(productId = productId))
        } catch (e: DataIntegrityViolationException) {
            log.debug("ProductMetrics 동시 생성 충돌, 이미 존재: productId={}", productId)
        }
    }
}
