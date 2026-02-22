package com.loopers.interfaces.api.brand

import com.loopers.application.brand.BrandInfo

class BrandV1AdminDto {
    data class RegisterRequest(
        val name: String,
        val description: String? = null,
        val logoUrl: String? = null,
    )

    data class BrandResponse(
        val id: Long,
        val name: String
    ) {
        companion object {
            fun from(info: BrandInfo): BrandResponse {
                return BrandResponse(
                    id = info.id,
                    name = info.name,
                )
            }
        }
    }

    data class BrandDetailResponse(
        val id: Long,
        val name: String,
        val description: String?,
        val logoUrl: String?,
    ) {
        companion object {
            fun from(info: BrandInfo): BrandDetailResponse {
                return BrandDetailResponse(
                    id = info.id,
                    name = info.name,
                    description = info.description,
                    logoUrl = info.logoUrl,
                )
            }
        }
    }
}
