package com.loopers.interfaces.api.catalog

import com.loopers.domain.catalog.BrandInfo
import org.springframework.data.domain.Slice

class BrandV1AdminDto {
    data class RegisterRequest(
        val name: String,
        val description: String? = null,
        val logoUrl: String? = null,
    )

    data class BrandResponse(
        val id: Long,
        val name: String,
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

    data class BrandSliceResponse(
        val content: List<BrandResponse>,
        val page: Int,
        val size: Int,
        val hasNext: Boolean,
    ) {
        companion object {
            fun from(slice: Slice<BrandInfo>): BrandSliceResponse {
                return BrandSliceResponse(
                    content = slice.content.map { BrandResponse.from(it) },
                    page = slice.number,
                    size = slice.size,
                    hasNext = slice.hasNext(),
                )
            }
        }
    }

    data class UpdateRequest(
        val newName: String? = null,
        val newDescription: String? = null,
        val newLogoUrl: String? = null,
    )
}
