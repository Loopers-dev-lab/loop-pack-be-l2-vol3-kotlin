package com.loopers.application.catalog.brand

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.CatalogService
import org.springframework.stereotype.Component

@Component
class GetBrandsUseCase(private val catalogService: CatalogService) {
    fun execute(page: Int, size: Int): PageResult<BrandInfo> {
        return catalogService.getBrands(page, size).map { BrandInfo.from(it) }
    }
}
