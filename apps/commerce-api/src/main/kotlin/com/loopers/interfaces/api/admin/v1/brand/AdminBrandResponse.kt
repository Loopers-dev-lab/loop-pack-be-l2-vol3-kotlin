package com.loopers.interfaces.api.admin.v1.brand

import com.loopers.application.brand.BrandInfo
import java.time.ZonedDateTime

data class AdminBrandResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val logoUrl: String?,
    val status: String,
    val deletedAt: ZonedDateTime?,
) {
    companion object {
        fun from(brandInfo: BrandInfo) = AdminBrandResponse(
            id = brandInfo.id,
            name = brandInfo.name,
            description = brandInfo.description,
            logoUrl = brandInfo.logoUrl,
            status = brandInfo.status,
            deletedAt = brandInfo.deletedAt,
        )
    }
}
