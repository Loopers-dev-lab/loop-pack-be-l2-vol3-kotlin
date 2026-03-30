package com.loopers.infrastructure.order

import com.loopers.domain.order.StockReservationRepository
import com.loopers.domain.product.ProductRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class StockCacheWarmer(
    private val productRepository: ProductRepository,
    private val stockReservationRepository: StockReservationRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun warmUp() {
        val products = productRepository.findAllActive()
        products.forEach { product ->
            stockReservationRepository.setStock(product.id, product.stockQuantity.value)
        }
        log.info("[StockCacheWarmer] Redis 재고 캐시 워밍 완료: {}건", products.size)
    }
}
