package com.loopers.interfaces.api.catalog

import com.loopers.application.catalog.GetBrandResult
import com.loopers.application.catalog.ListBrandsResult

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
            fun from(result: GetBrandResult): BrandResponse {
                return BrandResponse(
                    id = result.id,
                    name = result.name,
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
            fun from(result: GetBrandResult): BrandDetailResponse {
                return BrandDetailResponse(
                    id = result.id,
                    name = result.name,
                    description = result.description,
                    logoUrl = result.logoUrl,
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
            fun from(result: ListBrandsResult): BrandSliceResponse {
                return BrandSliceResponse(
                    content = result.content.map { BrandResponse.from(it) },
                    page = result.page,
                    size = result.size,
                    hasNext = result.hasNext,
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
