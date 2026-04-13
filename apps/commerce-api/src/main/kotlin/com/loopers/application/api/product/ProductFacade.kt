package com.loopers.application.api.product

import com.loopers.domain.product.ProductService
import com.loopers.domain.product.dto.ProductInfo
import com.loopers.domain.ranking.ProductRankingReadService
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProductFacade(
    private val productService: ProductService,
    private val productRankingReadService: ProductRankingReadService,
) {
    /**
     * 캐시된 상품 정보를 조회합니다.
     * 순수 조회만 수행하며, 조회 기록은 별도로 기록해야 합니다.
     */
    @Cacheable(value = ["product-info"], sync = true, key = "#id")
    fun getCachedProductInfo(id: Long): ProductInfo = productService.getProductInfo(id)

    /**
     * 상품 정보를 조회하고 오늘의 랭킹을 포함합니다.
     * rank는 캐시 밖에서 Redis에서 별도 조회 후 enrichment합니다.
     */
    fun getProductInfoWithRank(id: Long): ProductInfo {
        val productInfo = getCachedProductInfo(id)
        val rank = productRankingReadService.getRank(processingDate = null, productId = id)
        return productInfo.copy(rank = rank)
    }

    /**
     * 상품 조회를 기록합니다.
     */
    fun recordProductView(id: Long, userId: Long) {
        productService.recordProductView(id, userId)
    }

    fun getActiveProducts(brandId: Long?, pageable: Pageable): Page<ProductInfo> =
        productService.getActiveProducts(brandId, pageable)
}
