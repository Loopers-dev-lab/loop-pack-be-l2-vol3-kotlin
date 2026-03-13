package com.loopers.application.product

import com.loopers.domain.product.ProductService
import com.loopers.support.common.PageQuery
import com.loopers.support.common.SortOrder
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ProductCacheWarmingScheduler(
    private val productCacheManager: ProductCacheManager,
    private val productService: ProductService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val TOP_BRAND_COUNT = 10
        private const val WARMING_PAGE_COUNT = 3
        private const val DEFAULT_PAGE_SIZE = 20
        private val SORT_ORDERS = listOf(
            SortOrder.UNSORTED,
            SortOrder.by("price", SortOrder.Direction.ASC),
            SortOrder.by("likes", SortOrder.Direction.DESC),
        )
    }

    @Scheduled(initialDelay = 0, fixedRate = 2 * 60 * 1000)
    fun warmProductListCache() {
        log.info("상품 목록 캐시 워밍 시작")

        val topBrandIds = productService.getTopBrandIdsByProductCount(TOP_BRAND_COUNT)
        val brandIds: List<Long?> = listOf(null) + topBrandIds

        var count = 0
        for (brandId in brandIds) {
            for (sortOrder in SORT_ORDERS) {
                for (page in 0 until WARMING_PAGE_COUNT) {
                    val pageQuery = PageQuery(page, DEFAULT_PAGE_SIZE, sortOrder)
                    productCacheManager.getProducts(brandId, pageQuery)
                    count++
                }
            }
        }

        log.info("상품 목록 캐시 워밍 완료: {}건 (전체 + 브랜드 {}개)", count, topBrandIds.size)
    }
}
