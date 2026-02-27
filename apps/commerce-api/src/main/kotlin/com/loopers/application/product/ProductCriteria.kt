package com.loopers.application.product

import java.math.BigDecimal

data class CreateProductCriteria(
    val brandId: Long,
    val name: String,
    val price: BigDecimal,
    val stock: Int,
    val description: String?,
    val imageUrl: String?,
)

data class UpdateProductCriteria(
    val name: String,
    val price: BigDecimal,
    val stock: Int,
    val description: String?,
    val imageUrl: String?,
)
