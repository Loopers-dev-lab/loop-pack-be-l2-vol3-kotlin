package com.loopers.application.catalog.brand

import com.loopers.domain.catalog.CatalogService
import org.springframework.stereotype.Component

@Component
class DeleteBrandUseCase(private val catalogService: CatalogService) {
    fun execute(brandId: Long) {
        catalogService.deleteBrand(brandId)
    }
}
