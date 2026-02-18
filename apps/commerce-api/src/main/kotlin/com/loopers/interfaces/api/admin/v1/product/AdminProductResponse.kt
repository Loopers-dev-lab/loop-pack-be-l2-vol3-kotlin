package com.loopers.interfaces.api.admin.v1.product

import com.loopers.application.product.ProductImageInfo
import com.loopers.application.product.ProductInfo
import java.time.ZonedDateTime

data class AdminProductResponse(
    val id: Long,
    val brandId: Long,
    val brandName: String,
    val brandLogoUrl: String?,
    val name: String,
    val description: String?,
    val price: Long,
    val stock: Int,
    val thumbnailUrl: String?,
    val status: String,
    val likeCount: Int,
    val deletedAt: ZonedDateTime?,
    val images: List<AdminProductImageResponse>,
) {
    companion object {
        fun from(productInfo: ProductInfo) = AdminProductResponse(
            id = productInfo.id,
            brandId = productInfo.brandId,
            brandName = productInfo.brandName,
            brandLogoUrl = productInfo.brandLogoUrl,
            name = productInfo.name,
            description = productInfo.description,
            price = productInfo.price,
            stock = productInfo.stock,
            thumbnailUrl = productInfo.thumbnailUrl,
            status = productInfo.status,
            likeCount = productInfo.likeCount,
            deletedAt = productInfo.deletedAt,
            images = productInfo.images.map { AdminProductImageResponse.from(it) },
        )
    }
}

data class AdminProductImageResponse(
    val id: Long,
    val imageUrl: String,
    val displayOrder: Int,
) {
    companion object {
        fun from(imageInfo: ProductImageInfo) = AdminProductImageResponse(
            id = imageInfo.id,
            imageUrl = imageInfo.imageUrl,
            displayOrder = imageInfo.displayOrder,
        )
    }
}
