package com.loopers.domain.product

import java.time.ZonedDateTime

class Product private constructor(
    val persistenceId: Long?,
    val brandId: Long,
    val name: ProductName,
    val description: String?,
    val price: Money,
    val stock: Stock,
    val thumbnailUrl: String?,
    val status: ProductStatus,
    val likeCount: Int,
    val deletedAt: ZonedDateTime?,
    val images: List<ProductImage>,
) {

    init {
        require(likeCount >= 0) { "좋아요 수는 0 이상이어야 합니다." }
    }

    fun update(
        name: ProductName,
        description: String?,
        price: Money,
        stock: Stock,
        thumbnailUrl: String?,
        status: ProductStatus,
        images: List<ProductImage>,
    ): Product {
        require(!isDeleted()) { "삭제된 상품은 수정할 수 없습니다." }
        return Product(
            persistenceId = persistenceId,
            brandId = brandId,
            name = name,
            description = description,
            price = price,
            stock = stock,
            thumbnailUrl = thumbnailUrl,
            status = status,
            likeCount = likeCount,
            deletedAt = deletedAt,
            images = images,
        )
    }

    fun delete(): Product {
        return Product(
            persistenceId = persistenceId,
            brandId = brandId,
            name = name,
            description = description,
            price = price,
            stock = stock,
            thumbnailUrl = thumbnailUrl,
            status = status,
            likeCount = likeCount,
            deletedAt = ZonedDateTime.now(),
            images = images,
        )
    }

    fun canOrder(quantity: Int): Boolean {
        return !isDeleted() && stock.isEnough(quantity)
    }

    fun isDeleted(): Boolean = deletedAt != null

    companion object {
        fun create(
            brandId: Long,
            name: ProductName,
            description: String?,
            price: Money,
            stock: Stock,
            thumbnailUrl: String?,
            images: List<ProductImage>,
        ): Product {
            return Product(
                persistenceId = null,
                brandId = brandId,
                name = name,
                description = description,
                price = price,
                stock = stock,
                thumbnailUrl = thumbnailUrl,
                status = ProductStatus.ACTIVE,
                likeCount = 0,
                deletedAt = null,
                images = images,
            )
        }

        fun reconstitute(
            persistenceId: Long,
            brandId: Long,
            name: ProductName,
            description: String?,
            price: Money,
            stock: Stock,
            thumbnailUrl: String?,
            status: ProductStatus,
            likeCount: Int,
            deletedAt: ZonedDateTime?,
            images: List<ProductImage>,
        ): Product {
            return Product(
                persistenceId = persistenceId,
                brandId = brandId,
                name = name,
                description = description,
                price = price,
                stock = stock,
                thumbnailUrl = thumbnailUrl,
                status = status,
                likeCount = likeCount,
                deletedAt = deletedAt,
                images = images,
            )
        }
    }
}
