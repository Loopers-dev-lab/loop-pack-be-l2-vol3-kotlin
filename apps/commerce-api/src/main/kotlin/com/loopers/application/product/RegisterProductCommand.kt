package com.loopers.application.product

data class RegisterProductCommand(
    val brandId: Long,
    val name: String,
    val description: String?,
    val price: Long,
    val stock: Int,
    val thumbnailUrl: String?,
    val images: List<ProductImageCommand>,
)

data class ProductImageCommand(
    val imageUrl: String,
    val displayOrder: Int,
)
