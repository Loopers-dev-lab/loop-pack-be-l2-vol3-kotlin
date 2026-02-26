package com.loopers.application.brand

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandStatus
import java.time.ZonedDateTime

data class BrandInfo(
    val id: Long,
    val name: String,
    val description: String,
    val imageUrl: String,
    val status: BrandStatus,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
) {
    companion object {
        fun from(model: BrandModel): BrandInfo {
            return BrandInfo(
                id = model.id,
                name = model.name,
                description = model.description,
                imageUrl = model.imageUrl,
                status = model.status,
                createdAt = model.createdAt,
                updatedAt = model.updatedAt,
            )
        }
    }
}
