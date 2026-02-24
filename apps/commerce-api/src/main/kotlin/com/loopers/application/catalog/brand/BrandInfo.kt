package com.loopers.application.catalog.brand

import com.loopers.domain.catalog.brand.model.Brand
import java.time.ZonedDateTime

data class BrandInfo(
    val id: Long,
    val name: String,
    val deletedAt: ZonedDateTime?,
) {
    companion object {
        fun from(brand: Brand): BrandInfo = BrandInfo(
            id = brand.id,
            name = brand.name.value,
            deletedAt = brand.deletedAt,
        )
    }
}
