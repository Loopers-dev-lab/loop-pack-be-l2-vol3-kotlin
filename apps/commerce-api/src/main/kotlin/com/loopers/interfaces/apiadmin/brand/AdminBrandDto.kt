package com.loopers.interfaces.apiadmin.brand

import com.loopers.application.brand.BrandInfo

class AdminBrandDto {
    data class CreateRequest(
        val name: String,
        val description: String?,
    )

    data class CreateResponse(
        val id: Long,
        val name: String,
        val description: String?,
    ) {
        companion object {
            fun from(info: BrandInfo): CreateResponse {
                return CreateResponse(
                    id = info.id,
                    name = info.name,
                    description = info.description,
                )
            }
        }
    }
}
