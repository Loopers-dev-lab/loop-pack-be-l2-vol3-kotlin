package com.loopers.application.brand

import com.loopers.domain.brand.BrandService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class AdminBrandFacade(
    private val brandService: BrandService,
) {
    fun createBrand(name: String, description: String?): BrandInfo {
        return brandService.createBrand(name, description)
            .let { BrandInfo.from(it) }
    }

    fun getBrands(pageable: Pageable): Page<BrandInfo> {
        return brandService.getBrands(pageable)
            .map { BrandInfo.from(it) }
    }
}
