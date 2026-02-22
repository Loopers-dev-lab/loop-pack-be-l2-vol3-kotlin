package com.loopers.domain.product

class ProductCommand {
    data class Create(
        val brandId: Long,
        val name: String,
        val description: String,
        val price: Long,
        val stockQuantity: Int,
        val imageUrl: String,
    )

    data class Update(
        val name: String,
        val description: String,
        val price: Long,
        val stockQuantity: Int,
        val imageUrl: String,
    )
}
