package com.loopers.domain.productlike.dto

import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStatus
import java.math.BigDecimal

data class LikedProductInfo(
    val id: Long,
    val name: String,
    val price: BigDecimal,
    val stock: Int,
    val status: ProductStatus,
    val brandId: Long,
    val brandName: String,
    val likeCount: Int,
) {
    companion object {
        fun from(product: Product): LikedProductInfo {
            return LikedProductInfo(
                id = product.id,
                name = product.name,
                price = product.price,
                stock = product.stock,
                status = product.status,
                brandId = product.brand.id,
                brandName = product.brand.name,
                likeCount = product.likeCount,
            )
        }
    }
}
