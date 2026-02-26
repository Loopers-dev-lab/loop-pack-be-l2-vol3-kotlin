package com.loopers.interfaces.api.catalog

import com.loopers.application.catalog.GetBrandResult

class BrandV1Dto {
    data class BrandDetailResponse(
        val id: Long,
        val name: String,
        val description: String?,
        val logoUrl: String?,
    ) {
        companion object {
            fun from(result: GetBrandResult): BrandDetailResponse {
                return BrandDetailResponse(
                    id = result.id,
                    name = result.name,
                    description = result.description,
                    logoUrl = result.logoUrl,
                )
            }
        }
    }
}
