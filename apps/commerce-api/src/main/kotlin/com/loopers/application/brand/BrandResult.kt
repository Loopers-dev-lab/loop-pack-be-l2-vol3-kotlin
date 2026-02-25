package com.loopers.application.brand

import com.loopers.domain.brand.BrandInfo
import java.time.ZonedDateTime

data class BrandResult(
    val id: Long,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(info: BrandInfo): BrandResult {
            return BrandResult(
                id = info.id,
                name = info.name,
                description = info.description,
                imageUrl = info.imageUrl,
                createdAt = info.createdAt,
            )
        }
    }
}
