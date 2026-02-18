package com.loopers.application.brand

data class UpdateBrandCommand(
    val name: String,
    val description: String?,
    val logoUrl: String?,
)
