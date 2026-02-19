package com.loopers.interfaces.api.brand.dto

import com.loopers.domain.catalog.CatalogCommand
import com.loopers.domain.catalog.brand.entity.Brand
import jakarta.validation.constraints.NotBlank
import java.time.ZonedDateTime

class BrandAdminV1Dto {
    data class CreateBrandRequest(
        @field:NotBlank(message = "브랜드명은 필수입니다.")
        val name: String,
    ) {
        fun toCommand(): CatalogCommand.CreateBrand {
            return CatalogCommand.CreateBrand(name = name)
        }
    }

    data class UpdateBrandRequest(
        @field:NotBlank(message = "브랜드명은 필수입니다.")
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
