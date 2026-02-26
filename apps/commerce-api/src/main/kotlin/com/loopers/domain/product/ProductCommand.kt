package com.loopers.domain.product

data class CreateProductCommand(
    val brandId: Long,
    val name: String,
    val description: String?,
    val price: Long,
    val stockQuantity: Int,
    val displayYn: Boolean,
    val imageUrl: String?,
)

data class UpdateProductCommand(
    val name: String,
    val description: String?,
    val price: Long,
    val stockQuantity: Int,
    val status: ProductStatus,
    val displayYn: Boolean,
    val imageUrl: String?,
)
