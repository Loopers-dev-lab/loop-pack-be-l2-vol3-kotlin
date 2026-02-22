package com.loopers.application.brand

import com.loopers.domain.brand.BrandService
import org.springframework.stereotype.Component

@Component
class BrandFacade(
    private val brandService: BrandService,
) {
    fun getBrand(id: Long): BrandInfo {
        val brand = brandService.getBrand(id)
        return BrandInfo.from(brand)
    }
}
