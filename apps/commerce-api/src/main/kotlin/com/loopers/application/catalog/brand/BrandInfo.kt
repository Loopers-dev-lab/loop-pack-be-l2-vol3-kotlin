package com.loopers.application.catalog.brand

import com.loopers.domain.catalog.brand.model.Brand
import java.time.ZonedDateTime

data class BrandInfo(
    val id: Long,
    val name: String,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val deletedAt: ZonedDateTime?,
) {
    companion object {
        fun from(brand: Brand): BrandInfo = BrandInfo(
            id = brand.id,
            name = brand.name.value,
            createdAt = brand.createdAt,
            updatedAt = brand.updatedAt,
            deletedAt = brand.deletedAt,
        )
    }
}
