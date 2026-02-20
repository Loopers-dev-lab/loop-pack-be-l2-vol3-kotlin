package com.loopers.interfaces.api.brand.dto

import com.loopers.domain.catalog.brand.entity.Brand
import java.time.ZonedDateTime

class BrandAdminV1Dto {
    data class BrandAdminResponse(
        val id: Long,
        val name: String,
        val createdAt: ZonedDateTime,
        val updatedAt: ZonedDateTime,
        val deletedAt: ZonedDateTime?,
    ) {
        companion object {
            fun from(brand: Brand): BrandAdminResponse {
                return BrandAdminResponse(
                    id = brand.id,
                    name = brand.name,
                    createdAt = brand.createdAt,
                    updatedAt = brand.updatedAt,
                    deletedAt = brand.deletedAt,
                )
            }
        }
    }
}
