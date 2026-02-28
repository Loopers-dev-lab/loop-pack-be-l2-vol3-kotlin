package com.loopers.application.product

data class UpdateProductCommand(
    val name: String,
    val description: String?,
    val price: Long,
    val stock: Int,
    val thumbnailUrl: String?,
    val status: String,
    val images: List<ProductImageCommand>,
)
