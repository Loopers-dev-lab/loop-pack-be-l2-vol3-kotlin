package com.loopers.application.catalog.brand

import com.loopers.domain.catalog.CatalogService
import org.springframework.stereotype.Component

@Component
class RestoreBrandUseCase(private val catalogService: CatalogService) {
    fun execute(brandId: Long): BrandInfo {
        val brand = catalogService.restoreBrand(brandId)
        return BrandInfo.from(brand)
    }
}
