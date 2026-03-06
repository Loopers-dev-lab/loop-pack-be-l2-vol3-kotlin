package com.loopers.interfaces.api.brand

import com.loopers.application.brand.BrandInfo

class BrandV1Dto {
    data class BrandResponse(
        val id: Long,
        val name: String,
        val description: String,
    ) {
        companion object {
            fun from(brandInfo: BrandInfo): BrandResponse {
                return BrandResponse(
                    id = brandInfo.id,
                    name = brandInfo.name,
                    description = brandInfo.description,
                )
            }
        }
    }
}
