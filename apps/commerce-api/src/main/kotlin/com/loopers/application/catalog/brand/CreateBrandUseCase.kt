package com.loopers.application.catalog.brand

import com.loopers.domain.catalog.CatalogCommand
import com.loopers.domain.catalog.CatalogService
import com.loopers.domain.catalog.brand.vo.BrandName
import org.springframework.stereotype.Component

@Component
class CreateBrandUseCase(private val catalogService: CatalogService) {
    fun execute(name: String): BrandInfo {
        val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName(name)))
        return BrandInfo.from(brand)
    }
}
