package com.loopers.domain.product

import com.loopers.domain.Money

data class CreateProductCommand(
    val brandId: Long,
    val name: String,
    val description: String?,
    val price: Money,
    val stockQuantity: Int,
    val displayYn: Boolean,
    val imageUrl: String?,
)

data class UpdateProductCommand(
    val name: String,
    val description: String?,
    val price: Money,
    val stockQuantity: Int,
    val status: ProductStatus,
    val displayYn: Boolean,
    val imageUrl: String?,
)
