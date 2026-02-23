package com.loopers.application.product

import com.loopers.application.brand.BrandInfo
import com.loopers.domain.product.ProductModel
import java.time.ZonedDateTime

data class ProductInfo(
    val id: Long,
    val brandId: Long,
    val name: String,
    val imageUrl: String,
    val description: String,
    val price: Long,
    val likeCount: Long,
    val brand: BrandInfo,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
) {
    companion object {
        fun from(model: ProductModel, brand: BrandInfo) = ProductInfo(
            id = model.id,
            brandId = model.brandId,
            name = model.name.value,
            imageUrl = model.imageUrl.value,
            description = model.description.value,
            price = model.price.value,
            likeCount = model.likeCount.value,
            brand = brand,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt,
        )
    }
}
