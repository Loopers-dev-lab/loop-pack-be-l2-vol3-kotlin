package com.loopers.infrastructure.cache

import com.loopers.domain.catalog.ProductCache
import com.loopers.domain.catalog.ProductService
import com.loopers.domain.catalog.ProductSortType
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class ProductCacheWarmer(
    private val productService: ProductService,
    private val productCache: ProductCache,
    private val cacheProperties: CacheProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun warmUp() {
        if (cacheProperties.stampede.strategy != StampedeStrategy.WARMUP) {
            return
        }

        log.info("상품 캐시 워밍 시작")

        // 인기순 첫 5페이지 사전 캐시
        for (page in 0 until 5) {
            productCache.searchProducts(
                sortType = ProductSortType.POPULAR,
                brandId = null,
                page = page,
                size = 20,
            ) {
                productService.getProducts(PageRequest.of(page, 20), null, ProductSortType.POPULAR)
            }
        }

        // 최신순 첫 3페이지 사전 캐시
        for (page in 0 until 3) {
            productCache.searchProducts(
                sortType = ProductSortType.LATEST,
                brandId = null,
                page = page,
                size = 20,
            ) {
                productService.getProducts(PageRequest.of(page, 20), null, ProductSortType.LATEST)
            }
        }

        log.info("상품 캐시 워밍 완료")
    }
}
