package com.loopers.application.catalog.product

import com.loopers.domain.catalog.CatalogService
import org.springframework.stereotype.Component

@Component
class GetProductDetailUseCase(private val catalogService: CatalogService) {
    fun execute(productId: Long): ProductDetailInfo {
        val detail = catalogService.getProductDetail(productId)
        return ProductDetailInfo.from(detail)
    }
}
