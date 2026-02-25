package com.loopers.domain.product

import java.math.BigDecimal

data class CreateProductCommand(
    val brandId: Long,
    val name: String,
    val price: BigDecimal,
    val stock: Int,
    val description: String?,
    val imageUrl: String?,
)

data class UpdateProductCommand(
    val name: String,
    val price: BigDecimal,
    val stock: Int,
    val description: String?,
    val imageUrl: String?,
)
