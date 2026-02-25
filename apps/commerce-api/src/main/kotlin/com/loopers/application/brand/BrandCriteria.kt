package com.loopers.application.brand

data class CreateBrandCriteria(
    val name: String,
    val description: String?,
    val imageUrl: String?,
)

data class UpdateBrandCriteria(
    val name: String,
    val description: String?,
    val imageUrl: String?,
)
