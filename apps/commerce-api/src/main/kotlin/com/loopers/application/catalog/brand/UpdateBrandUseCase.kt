package com.loopers.application.catalog.brand

import com.loopers.domain.catalog.CatalogCommand
import com.loopers.domain.catalog.CatalogService
import com.loopers.domain.catalog.brand.vo.BrandName
import org.springframework.stereotype.Component

@Component
class UpdateBrandUseCase(private val catalogService: CatalogService) {
    fun execute(brandId: Long, name: String): BrandInfo {
        val brand = catalogService.updateBrand(brandId, CatalogCommand.UpdateBrand(name = BrandName(name)))
        return BrandInfo.from(brand)
    }
}
