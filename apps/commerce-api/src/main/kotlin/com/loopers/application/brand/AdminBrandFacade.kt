package com.loopers.application.brand

import com.loopers.domain.brand.BrandService
import org.springframework.stereotype.Component

@Component
class AdminBrandFacade(
    private val brandService: BrandService,
) {
    fun createBrand(name: String, description: String?): BrandInfo {
        return brandService.createBrand(name, description)
            .let { BrandInfo.from(it) }
    }

    fun getBrand(brandId: Long): BrandInfo {
        return brandService.getBrand(brandId)
            .let { BrandInfo.from(it) }
    }
}
