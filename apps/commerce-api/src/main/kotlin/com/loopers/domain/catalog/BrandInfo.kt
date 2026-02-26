package com.loopers.domain.catalog

data class BrandInfo(
    val id: Long,
    val name: String,
    val description: String?,
    val logoUrl: String?,
) {
    companion object {
        fun from(model: BrandModel): BrandInfo {
            return BrandInfo(
                id = model.id,
                name = model.name,
                description = model.description,
                logoUrl = model.logoUrl,
            )
        }
    }
}
