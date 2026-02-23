package com.loopers.application.catalog.product

import com.loopers.domain.catalog.CatalogService
import org.springframework.stereotype.Component

@Component
class DeleteProductUseCase(private val catalogService: CatalogService) {
    fun execute(productId: Long) {
        catalogService.deleteProduct(productId)
    }
}
