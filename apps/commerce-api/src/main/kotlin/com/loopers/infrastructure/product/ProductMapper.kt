package com.loopers.infrastructure.product

import com.loopers.domain.product.Money
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductImage
import com.loopers.domain.product.ProductName
import com.loopers.domain.product.Stock

object ProductMapper {

    fun toDomain(entity: ProductEntity): Product {
        val id = requireNotNull(entity.id) {
            "ProductEntity.id가 null입니다. 저장된 Entity만 Domain으로 변환 가능합니다."
        }
        return Product.reconstitute(
            persistenceId = id,
            brandId = entity.brandId,
            name = ProductName(entity.name),
            description = entity.description,
            price = Money(entity.price),
            stock = Stock(entity.stock),
            thumbnailUrl = entity.thumbnailUrl,
            status = entity.status,
            likeCount = entity.likeCount,
            deletedAt = entity.deletedAt,
            images = entity.images.map { toImageDomain(it) },
        )
    }

    fun toEntity(domain: Product): ProductEntity {
        val entity = ProductEntity(
            id = domain.persistenceId,
            brandId = domain.brandId,
            name = domain.name.value,
            description = domain.description,
            price = domain.price.amount,
            stock = domain.stock.quantity,
            thumbnailUrl = domain.thumbnailUrl,
            status = domain.status,
            likeCount = domain.likeCount,
            deletedAt = domain.deletedAt,
        )
        domain.images.forEach { image ->
            entity.images.add(toImageEntity(image, entity))
        }
        return entity
    }

    private fun toImageDomain(entity: ProductImageEntity): ProductImage {
        val id = requireNotNull(entity.id) {
            "ProductImageEntity.id가 null입니다. 저장된 Entity만 Domain으로 변환 가능합니다."
        }
        return ProductImage.reconstitute(
            persistenceId = id,
            imageUrl = entity.imageUrl,
            displayOrder = entity.displayOrder,
        )
    }

    private fun toImageEntity(domain: ProductImage, productEntity: ProductEntity): ProductImageEntity {
        return ProductImageEntity(
            id = domain.persistenceId,
            product = productEntity,
            imageUrl = domain.imageUrl,
            displayOrder = domain.displayOrder,
        )
    }
}
