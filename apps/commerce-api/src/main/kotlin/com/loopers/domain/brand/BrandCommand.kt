package com.loopers.domain.brand

data class CreateBrandCommand(
    val name: String,
    val description: String?,
    val imageUrl: String?,
)

data class UpdateBrandCommand(
    val name: String,
    val description: String?,
    val imageUrl: String?,
)
