package com.loopers.application.catalog.brand

import com.loopers.domain.catalog.CatalogService
import org.springframework.stereotype.Component

@Component
class GetBrandUseCase(private val catalogService: CatalogService) {
    fun executeActive(brandId: Long): BrandInfo {
        val brand = catalogService.getActiveBrand(brandId)
        return BrandInfo.from(brand)
    }

    fun executeAdmin(brandId: Long): BrandInfo {
        val brand = catalogService.getBrand(brandId)
        return BrandInfo.from(brand)
    }
}
