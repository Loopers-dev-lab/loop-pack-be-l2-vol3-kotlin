package com.loopers.interfaces.api.brand

import com.loopers.application.brand.BrandInfo

class BrandDto {
    data class DetailResponse(
        val id: Long,
        val name: String,
        val description: String?,
    ) {
        companion object {
            fun from(info: BrandInfo): DetailResponse {
                return DetailResponse(
                    id = info.id,
                    name = info.name,
                    description = info.description,
                )
            }
        }
    }
}
