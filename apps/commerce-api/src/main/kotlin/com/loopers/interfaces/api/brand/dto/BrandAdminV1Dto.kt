package com.loopers.interfaces.api.brand.dto

import com.loopers.application.catalog.brand.BrandInfo
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
            fun from(info: BrandInfo): BrandAdminResponse {
                return BrandAdminResponse(
                    id = info.id,
                    name = info.name,
                    createdAt = info.createdAt,
                    updatedAt = info.updatedAt,
                    deletedAt = info.deletedAt,
                )
            }
        }
    }
}
