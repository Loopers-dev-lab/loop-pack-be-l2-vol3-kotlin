package com.loopers.application.product

data class CreateProductCriteria(
    val brandId: Long,
    val name: String,
    val description: String?,
    val price: Long,
    val stockQuantity: Int,
    val displayYn: Boolean,
    val imageUrl: String?,
)

data class UpdateProductCriteria(
    val name: String,
    val description: String?,
    val price: Long,
    val stockQuantity: Int,
    val status: String,
    val displayYn: Boolean,
    val imageUrl: String?,
)
