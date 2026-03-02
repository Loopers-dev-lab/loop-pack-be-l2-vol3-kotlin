package com.loopers.interfaces.api.admin.brand

import com.loopers.application.brand.BrandInfo

data class AdminBrandResponse(
    val id: Long,
    val name: String,
) {
    companion object {
        fun from(info: BrandInfo): AdminBrandResponse {
            return AdminBrandResponse(
                id = info.id,
                name = info.name,
            )
        }
    }
}
