package com.loopers.application.catalog

data class RegisterBrandCriteria(
    val name: String,
    val description: String? = null,
    val logoUrl: String? = null,
)

data class UpdateBrandCriteria(
    val brandId: Long,
    val newName: String? = null,
    val newDescription: String? = null,
    val newLogoUrl: String? = null,
)

data class ListBrandsCriteria(
    val page: Int,
    val size: Int,
)
