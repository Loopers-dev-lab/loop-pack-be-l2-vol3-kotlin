package com.loopers.application.user.like

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStock
import java.math.BigDecimal

class UserProductLikeResult {
    data class LikedProduct(
        val productId: Long,
        val productName: String,
        val sellingPrice: BigDecimal,
        val brandId: Long,
        val brandName: String,
        val thumbnailUrl: String?,
        val likeCount: Int,
        val soldOut: Boolean,
    ) {
        companion object {
            fun from(product: Product, brand: Brand, stock: ProductStock?): LikedProduct =
                LikedProduct(
                    productId = product.id!!,
                    productName = product.name,
                    sellingPrice = product.sellingPrice.amount,
                    brandId = product.brandId,
                    brandName = brand.name.value,
                    thumbnailUrl = product.thumbnailUrl,
                    likeCount = product.likeCount,
                    soldOut = stock == null || stock.quantity.value == 0,
                )
        }
    }
}
