package com.loopers.interfaces.api.brand

import com.loopers.application.brand.BrandResult
import com.loopers.application.brand.CreateBrandCriteria
import com.loopers.application.brand.UpdateBrandCriteria
import java.time.ZonedDateTime

class BrandV1Dto {

    data class CreateRequest(
        val name: String,
        val description: String?,
        val imageUrl: String?,
    ) {
        fun toCriteria(): CreateBrandCriteria {
            return CreateBrandCriteria(
                name = name,
                description = description,
                imageUrl = imageUrl,
            )
        }
    }

    data class UpdateRequest(
        val name: String,
        val description: String?,
        val imageUrl: String?,
    ) {
        fun toCriteria(): UpdateBrandCriteria {
            return UpdateBrandCriteria(
                name = name,
                description = description,
                imageUrl = imageUrl,
            )
        }
    }

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
