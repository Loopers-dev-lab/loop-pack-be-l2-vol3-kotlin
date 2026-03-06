package com.loopers.domain.brand.dto

import com.loopers.domain.brand.Brand

data class BrandInfo(
    val brandId: Long,
    val brandName: String,
    val description: String,
) {
    companion object {
        fun from(brand: Brand): BrandInfo {
            return BrandInfo(
                brandId = brand.id,
                brandName = brand.name,
                description = brand.description,
            )
        }
    }
}
