package com.loopers.infrastructure.product

import com.loopers.domain.common.Money
import com.loopers.domain.product.Product
import org.springframework.stereotype.Component

@Component
class ProductMapper {
    fun toDomain(entity: ProductEntity): Product {
        return Product.retrieve(
            id = entity.id!!,
            name = entity.name,
            regularPrice = Money(entity.regularPrice),
            sellingPrice = Money(entity.sellingPrice),
            brandId = entity.brandId,
            imageUrl = entity.imageUrl,
            thumbnailUrl = entity.thumbnailUrl,
            likeCount = entity.likeCount,
            status = entity.status,
        )
    }

    fun toEntity(product: Product, admin: String): ProductEntity {
        return ProductEntity(
            id = product.id,
            name = product.name,
            regularPrice = product.regularPrice.amount,
            sellingPrice = product.sellingPrice.amount,
            brandId = product.brandId,
            imageUrl = product.imageUrl,
            thumbnailUrl = product.thumbnailUrl,
            likeCount = product.likeCount,
            status = product.status,
            createdBy = admin,
            updatedBy = admin,
        )
    }
}
