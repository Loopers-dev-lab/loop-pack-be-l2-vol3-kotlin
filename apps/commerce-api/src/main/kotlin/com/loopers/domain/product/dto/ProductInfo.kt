package com.loopers.domain.product.dto

import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStatus
import java.math.BigDecimal

data class ProductInfo(
    val id: Long,
    val name: String,
    val price: BigDecimal,
    val stock: Int,
    val status: ProductStatus,
    val brandId: Long,
    val brandName: String,
) {
    companion object {
        fun from(product: Product): ProductInfo {
            return ProductInfo(
                id = product.id,
                name = product.name,
                price = product.price,
                stock = product.stock,
                status = product.status,
                brandId = product.brand.id,
                brandName = product.brand.name,
            )
        }
    }
}
