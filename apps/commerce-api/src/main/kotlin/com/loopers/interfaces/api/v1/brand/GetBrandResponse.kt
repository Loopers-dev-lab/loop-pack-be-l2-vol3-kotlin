package com.loopers.interfaces.api.v1.brand

import com.loopers.application.brand.BrandInfo

data class GetBrandResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val logoUrl: String?,
) {
    companion object {
        fun from(brandInfo: BrandInfo) = GetBrandResponse(
            id = brandInfo.id,
            name = brandInfo.name,
            description = brandInfo.description,
            logoUrl = brandInfo.logoUrl,
        )
    }
}
