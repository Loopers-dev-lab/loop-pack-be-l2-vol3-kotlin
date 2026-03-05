package com.loopers.application.admin.product

import java.math.BigDecimal

class AdminProductCommand {
    data class Register(
        val name: String,
        val regularPrice: BigDecimal,
        val sellingPrice: BigDecimal,
        val brandId: Long,
        val initialStock: Int,
        val imageUrl: String?,
        val thumbnailUrl: String?,
        val admin: String,
    )

    data class Update(
        val productId: Long,
        val name: String,
        val regularPrice: BigDecimal,
        val sellingPrice: BigDecimal,
        val status: String,
        val imageUrl: String?,
        val thumbnailUrl: String?,
        val admin: String,
    )
}
