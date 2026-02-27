package com.loopers.interfaces.api.catalog

import com.loopers.application.catalog.UserGetBrandResult

class BrandV1Dto {
    data class BrandResponse(
        val id: Long,
        val name: String,
    ) {
        companion object {
            fun from(result: UserGetBrandResult): BrandResponse {
                return BrandResponse(
                    id = result.id,
                    name = result.name,
                )
            }
        }
    }
}
