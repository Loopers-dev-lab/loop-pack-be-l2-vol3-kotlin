package com.loopers.interfaces.api.brand

import com.loopers.application.brand.BrandResult
import java.time.ZonedDateTime

class BrandV1Dto {

    data class BrandResponse(
        val id: Long,
        val name: String,
        val description: String?,
        val imageUrl: String?,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: BrandResult): BrandResponse {
                return BrandResponse(
                    id = result.id,
                    name = result.name,
                    description = result.description,
                    imageUrl = result.imageUrl,
                    createdAt = result.createdAt,
                )
            }
        }
    }
}
