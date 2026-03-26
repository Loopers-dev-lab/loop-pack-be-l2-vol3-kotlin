package com.loopers.application.metrics

import com.loopers.infrastructure.event.EventHandledEntity
import com.loopers.infrastructure.event.EventHandledJpaRepository
import com.loopers.infrastructure.metrics.ProductMetricsEntity
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MetricsService(
    private val productMetricsJpaRepository: ProductMetricsJpaRepository,
    private val eventHandledJpaRepository: EventHandledJpaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun handleProductViewed(eventId: String, productId: Long) {
        if (isAlreadyHandled(eventId)) return

        val metrics = getOrCreate(productId)
        metrics.incrementView()
        productMetricsJpaRepository.save(metrics)
        markHandled(eventId)

        log.info("[Metrics] PRODUCT_VIEWED: productId=$productId, viewCount=${metrics.viewCount}")
    }

    @Transactional
    fun handleProductLiked(eventId: String, productId: Long) {
        if (isAlreadyHandled(eventId)) return

        val metrics = getOrCreate(productId)
        metrics.incrementLike()
        productMetricsJpaRepository.save(metrics)
        markHandled(eventId)

        log.info("[Metrics] PRODUCT_LIKED: productId=$productId, likeCount=${metrics.likeCount}")
    }

    @Transactional
    fun handleProductUnliked(eventId: String, productId: Long) {
        if (isAlreadyHandled(eventId)) return

        val metrics = getOrCreate(productId)
        metrics.decrementLike()
        productMetricsJpaRepository.save(metrics)
        markHandled(eventId)

        log.info("[Metrics] PRODUCT_UNLIKED: productId=$productId, likeCount=${metrics.likeCount}")
    }

    @Transactional
    fun handleOrderPlaced(eventId: String, items: List<OrderItemMetrics>) {
        if (isAlreadyHandled(eventId)) return

        for (item in items) {
            val metrics = getOrCreate(item.productId)
            metrics.addSales(item.quantity, item.price * item.quantity)
            productMetricsJpaRepository.save(metrics)
        }
        markHandled(eventId)

        log.info("[Metrics] ORDER_PLACED: ${items.size} items processed")
    }

    private fun getOrCreate(productId: Long): ProductMetricsEntity =
        productMetricsJpaRepository.findByProductId(productId)
            ?: ProductMetricsEntity(productId = productId)

    private fun isAlreadyHandled(eventId: String): Boolean {
        val exists = eventHandledJpaRepository.existsById(eventId)
        if (exists) {
            log.info("[Metrics] 이미 처리된 이벤트 skip: eventId=$eventId")
        }
        return exists
    }

    private fun markHandled(eventId: String) {
        eventHandledJpaRepository.save(EventHandledEntity(eventId = eventId))
    }
}

data class OrderItemMetrics(
    val productId: Long,
    val quantity: Int,
    val price: Int,
)
