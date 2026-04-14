package com.loopers.application.metrics

import com.loopers.domain.idempotency.EventHandled
import com.loopers.domain.idempotency.EventHandledRepository
import com.loopers.domain.metrics.ProductMetrics
import com.loopers.domain.metrics.ProductMetricsRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MetricsEventProcessor(
    private val productMetricsRepository: ProductMetricsRepository,
    private val eventHandledRepository: EventHandledRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun processProductViewed(eventId: String, eventType: String, productId: Long) {
        if (isDuplicate(eventId)) return

        val metrics = findOrCreate(productId)
        metrics.incrementViewCount()
        markHandled(eventId, eventType)

        log.info("조회 metrics 반영 [productId={}, viewCount={}]", productId, metrics.viewCount)
    }

    @Transactional
    fun processLikeCreated(eventId: String, eventType: String, productId: Long) {
        if (isDuplicate(eventId)) return

        val metrics = findOrCreate(productId)
        metrics.incrementLikeCount()
        markHandled(eventId, eventType)

        log.info("좋아요 metrics 반영 [productId={}, likeCount={}]", productId, metrics.likeCount)
    }

    @Transactional
    fun processLikeCancelled(eventId: String, eventType: String, productId: Long) {
        if (isDuplicate(eventId)) return

        val metrics = findOrCreate(productId)
        metrics.decrementLikeCount()
        markHandled(eventId, eventType)

        log.info("좋아요 취소 metrics 반영 [productId={}, likeCount={}]", productId, metrics.likeCount)
    }

    @Transactional
    fun processOrderCreated(eventId: String, eventType: String, productIds: List<Long>, totalAmount: Long) {
        if (isDuplicate(eventId)) return

        productIds.forEach { productId ->
            val metrics = findOrCreate(productId)
            metrics.incrementOrderCount(productCount = 1, revenue = 0)
        }
        markHandled(eventId, eventType)

        log.info("주문 생성 metrics 반영 [productIds={}]", productIds)
    }

    @Transactional
    fun processPaymentApproved(eventId: String, eventType: String, productIds: List<Long>, amount: Long) {
        if (isDuplicate(eventId)) return

        val revenuePerProduct = if (productIds.isNotEmpty()) amount / productIds.size else 0L
        productIds.forEach { productId ->
            val metrics = findOrCreate(productId)
            metrics.incrementOrderCount(productCount = 0, revenue = revenuePerProduct)
        }
        markHandled(eventId, eventType)

        log.info("결제 승인 metrics 반영 [productIds={}, amount={}]", productIds, amount)
    }

    private fun isDuplicate(eventId: String): Boolean {
        if (eventHandledRepository.existsByEventId(eventId)) {
            log.info("중복 이벤트 skip [eventId={}]", eventId)
            return true
        }
        return false
    }

    private fun findOrCreate(productId: Long): ProductMetrics {
        return productMetricsRepository.findByProductId(productId)
            ?: try {
                productMetricsRepository.save(ProductMetrics.create(productId))
            } catch (e: DataIntegrityViolationException) {
                productMetricsRepository.findByProductId(productId)
                    ?: throw e
            }
    }

    private fun markHandled(eventId: String, eventType: String) {
        eventHandledRepository.save(EventHandled.create(eventId, eventType))
    }
}
