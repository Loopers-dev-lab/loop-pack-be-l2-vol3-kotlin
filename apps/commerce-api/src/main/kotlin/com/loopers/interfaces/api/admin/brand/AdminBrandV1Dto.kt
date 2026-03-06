package com.loopers.interfaces.api.admin.brand

import com.loopers.application.brand.BrandInfo

class AdminBrandV1Dto {
    data class CreateBrandRequest(
        val name: String,
        val description: String,
    )

    data class UpdateBrandRequest(
        val name: String,
        val description: String,
    )

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
