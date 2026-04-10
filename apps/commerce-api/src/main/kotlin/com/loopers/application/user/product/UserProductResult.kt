package com.loopers.application.user.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductQueryResult
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
        val rank: Long? = null,
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

            fun from(result: ProductQueryResult.Detail): Detail = Detail(
                id = result.id,
                name = result.name,
                regularPrice = result.regularPrice,
                sellingPrice = result.sellingPrice,
                brandId = result.brandId,
                brandName = result.brandName,
                imageUrl = result.imageUrl,
                thumbnailUrl = result.thumbnailUrl,
                likeCount = result.likeCount,
                stockQuantity = result.stockQuantity,
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

            fun from(result: ProductQueryResult.Summary): Summary = Summary(
                id = result.id,
                name = result.name,
                sellingPrice = result.sellingPrice,
                brandId = result.brandId,
                brandName = result.brandName,
                thumbnailUrl = result.thumbnailUrl,
                likeCount = result.likeCount,
            )
        }
    }
}
