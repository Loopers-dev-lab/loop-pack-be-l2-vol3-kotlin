package com.loopers.application.api.product

import com.loopers.domain.product.dto.ProductInfo
import com.loopers.domain.ranking.ProductRankingReadService
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ProductRankingFacade(
    private val productFacade: ProductFacade,
    private val productRankingReadService: ProductRankingReadService,
) {
    fun getRankedProducts(processingDate: LocalDate?, page: Int, size: Int): PageImpl<ProductInfo> {
        val rankedProducts = productRankingReadService.getRankedProducts(processingDate, page, size)
        val content = rankedProducts.mapNotNull { rankedProduct ->
            runCatching {
                productFacade.getCachedProductInfo(rankedProduct.productId)
                    .copy(rank = rankedProduct.rank)
            }.getOrNull()
        }
        val pageable = PageRequest.of(page, size)
        val total = productRankingReadService.count(processingDate)
        return PageImpl(content, pageable, total)
    }
}
