package com.loopers.interfaces.api.brand

import com.loopers.domain.catalog.CatalogCommand
import com.loopers.domain.catalog.brand.Brand
import java.time.ZonedDateTime

class BrandAdminV1Dto {
    data class CreateBrandRequest(
        val name: String,
    ) {
        fun toCommand(): CatalogCommand.CreateBrand {
            return CatalogCommand.CreateBrand(name = name)
        }
    }

    data class UpdateBrandRequest(
        val name: String,
    ) {
        fun toCommand(): CatalogCommand.UpdateBrand {
            return CatalogCommand.UpdateBrand(name = name)
        }
    }

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
