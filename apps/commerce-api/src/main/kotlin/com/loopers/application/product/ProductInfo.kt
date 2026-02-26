package com.loopers.application.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product

data class ProductInfo(
    val id: Long,
    val name: String,
    val description: String?,
    val price: Long,
    val likes: Int,
    val stockQuantity: Int,
    val brandId: Long,
) {
    companion object {
        fun from(product: Product): ProductInfo {
            return ProductInfo(
                id = product.id,
                name = product.name,
                description = product.description,
                price = product.price.value,
                likes = product.likes.value,
                stockQuantity = product.stockQuantity.value,
                brandId = product.brandId,
            )
        }
    }
}

data class ProductDetailInfo(
    val id: Long,
    val name: String,
    val price: Long,
    val description: String?,
    val brandId: Long,
    val brandName: String,
    val likeCount: Int,
) {
    companion object {
        fun from(product: Product, brand: Brand): ProductDetailInfo {
            return ProductDetailInfo(
                id = product.id,
                name = product.name,
                price = product.price.value,
                description = product.description,
                brandId = brand.id,
                brandName = brand.name,
                likeCount = product.likes.value,
            )
        }
    }
}
