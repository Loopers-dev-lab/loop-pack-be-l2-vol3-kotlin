package com.loopers.application.catalog.product

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.CatalogService
import org.springframework.stereotype.Component

@Component
class GetProductsAdminUseCase(private val catalogService: CatalogService) {
    fun execute(page: Int, size: Int): PageResult<ProductInfo> {
        return catalogService.getAdminProducts(page, size).map { ProductInfo.from(it) }
    }
}
