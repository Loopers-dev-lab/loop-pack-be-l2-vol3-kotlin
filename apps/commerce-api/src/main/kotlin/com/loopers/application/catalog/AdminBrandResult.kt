package com.loopers.application.catalog

import com.loopers.application.SliceResult
import com.loopers.domain.catalog.BrandInfo

data class RegisterBrandResult(
    val id: Long,
) {
    companion object {
        fun from(info: BrandInfo): RegisterBrandResult {
            return RegisterBrandResult(id = info.id)
        }
    }
}

data class GetBrandResult(
    val id: Long,
    val name: String,
    val description: String?,
    val logoUrl: String?,
) {
    companion object {
        fun from(info: BrandInfo): GetBrandResult {
            return GetBrandResult(
                id = info.id,
                name = info.name,
                description = info.description,
                logoUrl = info.logoUrl,
            )
        }
    }
}

data class ListBrandsResult(
    val content: List<GetBrandResult>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
) {
    companion object {
        fun from(sliceResult: SliceResult<GetBrandResult>): ListBrandsResult {
            return ListBrandsResult(
                content = sliceResult.content,
                page = sliceResult.page,
                size = sliceResult.size,
                hasNext = sliceResult.hasNext,
            )
        }
    }
}
