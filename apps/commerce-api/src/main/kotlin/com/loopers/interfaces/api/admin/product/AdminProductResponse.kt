package com.loopers.interfaces.api.admin.product

import com.loopers.application.product.ProductInfo
import java.time.ZonedDateTime

data class AdminProductResponse(
    val id: Long,
    val brandId: Long,
    val brandName: String,
    val name: String,
    val description: String,
    val price: Long,
    val stock: Int,
    val imageUrl: String,
    val likeCount: Long,
    val available: Boolean,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val deletedAt: ZonedDateTime?,
) {
    companion object {
        fun from(info: ProductInfo): AdminProductResponse {
            return AdminProductResponse(
                id = info.id,
                brandId = info.brandId,
                brandName = info.brandName,
                name = info.name,
                description = info.description,
                price = info.price,
                stock = info.stock,
                imageUrl = info.imageUrl,
                likeCount = info.likeCount,
                available = info.available,
                createdAt = info.createdAt,
                updatedAt = info.updatedAt,
                deletedAt = info.deletedAt,
            )
        }
    }
}
