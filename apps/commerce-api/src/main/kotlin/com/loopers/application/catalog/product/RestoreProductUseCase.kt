package com.loopers.application.catalog.product

import com.loopers.domain.catalog.CatalogService
import org.springframework.stereotype.Component

@Component
class RestoreProductUseCase(private val catalogService: CatalogService) {
    fun execute(productId: Long): ProductInfo {
        val product = catalogService.restoreProduct(productId)
        return ProductInfo.from(product)
    }
}
