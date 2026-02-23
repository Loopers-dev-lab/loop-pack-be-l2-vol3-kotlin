package com.loopers.application.catalog.product

import com.loopers.domain.catalog.CatalogService
import org.springframework.stereotype.Component

@Component
class GetProductUseCase(private val catalogService: CatalogService) {
    fun executeAdmin(productId: Long): ProductInfo {
        val product = catalogService.getAdminProduct(productId)
        return ProductInfo.from(product)
    }
}
