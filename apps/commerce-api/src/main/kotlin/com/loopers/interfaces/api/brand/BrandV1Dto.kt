package com.loopers.interfaces.api.brand

import com.loopers.domain.catalog.brand.Brand

class BrandV1Dto {
    data class BrandResponse(
        val id: Long,
        val name: String,
    ) {
        companion object {
            fun from(brand: Brand): BrandResponse {
                return BrandResponse(
                    id = brand.id,
                    name = brand.name,
                )
            }
        }
    }
}
