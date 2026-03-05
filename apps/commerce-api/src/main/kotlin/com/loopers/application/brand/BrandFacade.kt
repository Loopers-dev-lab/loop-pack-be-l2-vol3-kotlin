package com.loopers.application.brand

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrandFacade(
    private val brandService: BrandService,
) {
    @Transactional(readOnly = true)
    fun getBrand(id: Long): BrandInfo {
        val brand = brandService.getBrand(id)
        return BrandInfo.from(brand)
    }
}
