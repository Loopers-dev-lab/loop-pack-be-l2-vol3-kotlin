package com.loopers.application.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductImage
import java.time.ZonedDateTime

data class ProductInfo(
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
    val images: List<ProductImageInfo>,
) {
    companion object {
        fun from(product: Product, brand: Brand): ProductInfo {
            val id = requireNotNull(product.persistenceId) {
                "Product.persistenceId가 null입니다. 저장된 Product만 매핑 가능합니다."
            }
            return ProductInfo(
                id = id,
                brandId = product.brandId,
                brandName = brand.name.value,
                brandLogoUrl = brand.logoUrl,
                name = product.name.value,
                description = product.description,
                price = product.price.amount,
                stock = product.stock.quantity,
                thumbnailUrl = product.thumbnailUrl,
                status = product.status.name,
                likeCount = product.likeCount,
                deletedAt = product.deletedAt,
                images = product.images.map { ProductImageInfo.from(it) },
            )
        }
    }
}

data class ProductImageInfo(
    val id: Long,
    val imageUrl: String,
    val displayOrder: Int,
) {
    companion object {
        fun from(image: ProductImage): ProductImageInfo {
            val id = requireNotNull(image.persistenceId) {
                "ProductImage.persistenceId가 null입니다."
            }
            return ProductImageInfo(
                id = id,
                imageUrl = image.imageUrl,
                displayOrder = image.displayOrder,
            )
        }
    }
}
