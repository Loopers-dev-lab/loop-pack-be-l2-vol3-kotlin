package com.loopers.application.api.product

import com.loopers.domain.product.dto.ProductInfo
import com.loopers.domain.ranking.ProductRankingReadService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ProductRankingFacade(
    private val productFacade: ProductFacade,
    private val productRankingReadService: ProductRankingReadService,
) {
    companion object {
        private val log = LoggerFactory.getLogger(ProductRankingFacade::class.java)
    }

    fun getRankedProducts(processingDate: LocalDate?, page: Int, size: Int): PageImpl<ProductInfo> {
        val result = productRankingReadService.getRankedProductsWithCount(processingDate, page, size)
        val content = result.products.mapNotNull { rankedProduct ->
            runCatching {
                productFacade.getCachedProductInfo(rankedProduct.productId)
                    .copy(rank = rankedProduct.rank)
            }.onFailure { ex ->
                log.warn(
                    "Failed to fetch product info for ranking. productId={}, rank={}, exception={}",
                    rankedProduct.productId,
                    rankedProduct.rank,
                    ex.javaClass.simpleName,
                )
            }.getOrNull()
        }
        val pageable = PageRequest.of(page, size)
        return PageImpl(content, pageable, result.count)
    }
}
