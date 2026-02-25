package com.loopers.domain.brand

import java.time.ZonedDateTime

data class BrandInfo(
    val id: Long,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(brand: Brand): BrandInfo {
            return BrandInfo(
                id = brand.id,
                name = brand.name,
                description = brand.description,
                imageUrl = brand.imageUrl,
                createdAt = brand.createdAt,
            )
        }
    }
}
