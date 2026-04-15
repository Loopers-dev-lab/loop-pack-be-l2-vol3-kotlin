package com.loopers.application.metrics

import com.loopers.domain.metrics.model.ProductMetrics
import com.loopers.domain.metrics.model.ProductMetricsDaily
import com.loopers.domain.metrics.repository.ProductMetricsDailyRepository
import com.loopers.domain.metrics.repository.ProductMetricsRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class ProductMetricsInitializer(
    private val productMetricsRepository: ProductMetricsRepository,
    private val productMetricsDailyRepository: ProductMetricsDailyRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun findOrCreate(productId: Long): ProductMetrics {
        return productMetricsRepository.findByProductId(productId)
            ?: try {
                productMetricsRepository.save(ProductMetrics(productId = productId))
            } catch (e: DataIntegrityViolationException) {
                productMetricsRepository.findByProductId(productId) ?: throw e
            }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun findOrCreateDaily(date: LocalDate, productId: Long): ProductMetricsDaily {
        return productMetricsDailyRepository.findByDateAndProductId(date, productId)
            ?: try {
                productMetricsDailyRepository.save(ProductMetricsDaily(productId = productId, metricDate = date))
            } catch (e: DataIntegrityViolationException) {
                productMetricsDailyRepository.findByDateAndProductId(date, productId) ?: throw e
            }
    }
}
