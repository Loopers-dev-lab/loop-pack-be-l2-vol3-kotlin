package com.loopers.application.user.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStock
import java.math.BigDecimal

class UserProductResult {
    data class Detail(
        val id: Long,
        val name: String,
        val regularPrice: BigDecimal,
        val sellingPrice: BigDecimal,
        val brandId: Long,
        val brandName: String,
        val imageUrl: String?,
        val thumbnailUrl: String?,
        val likeCount: Int,
        val stockQuantity: Int,
    ) {
        companion object {
            fun from(product: Product, brand: Brand, stock: ProductStock): Detail = Detail(
                id = product.id!!,
                name = product.name,
                regularPrice = product.regularPrice.amount,
                sellingPrice = product.sellingPrice.amount,
                brandId = product.brandId,
                brandName = brand.name.value,
                imageUrl = product.imageUrl,
                thumbnailUrl = product.thumbnailUrl,
                likeCount = product.likeCount,
                stockQuantity = stock.quantity.value,
            )
        }
    }

    data class Summary(
        val id: Long,
        val name: String,
        val sellingPrice: BigDecimal,
        val brandId: Long,
        val brandName: String,
        val thumbnailUrl: String?,
        val likeCount: Int,
    ) {
        companion object {
            fun from(product: Product, brand: Brand): Summary = Summary(
                id = product.id!!,
                name = product.name,
                sellingPrice = product.sellingPrice.amount,
                brandId = product.brandId,
                brandName = brand.name.value,
                thumbnailUrl = product.thumbnailUrl,
                likeCount = product.likeCount,
            )
        }
    }
}
