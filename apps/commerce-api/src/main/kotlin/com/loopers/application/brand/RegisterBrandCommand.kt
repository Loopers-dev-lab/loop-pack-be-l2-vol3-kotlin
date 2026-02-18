package com.loopers.application.brand

data class RegisterBrandCommand(
    val name: String,
    val description: String?,
    val logoUrl: String?,
)
