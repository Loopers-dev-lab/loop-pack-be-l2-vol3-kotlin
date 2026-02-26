package com.loopers.domain.catalog

import java.math.BigDecimal

data class ProductInfo(
    val id: Long,
    val brandId: Long,
    val name: String,
    val quantity: Int,
    val price: BigDecimal,
) {
    companion object {
        fun from(model: ProductModel): ProductInfo {
            return ProductInfo(
                id = model.id,
                brandId = model.brandId,
                name = model.name,
                quantity = model.quantity,
                price = model.price,
            )
        }
    }
}
