package com.loopers.infrastructure.product

import com.loopers.domain.BaseEntity
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "product")
class ProductJpaModel(
    brandId: Long,
    name: String,
    description: String,
    price: Long,
    stockQuantity: Int,
    imageUrl: String,
) : BaseEntity() {
    @Column(name = "brand_id", nullable = false)
    var brandId: Long = brandId
        protected set

    @Column(name = "name", nullable = false)
    var name: String = name
        protected set

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    var description: String = description
        protected set

    @Column(name = "price", nullable = false)
    var price: Long = price
        protected set

    @Column(name = "stock_quantity", nullable = false)
    var stockQuantity: Int = stockQuantity
        protected set

    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0
        protected set

    @Column(name = "image_url", nullable = false, length = 512)
    var imageUrl: String = imageUrl
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: ProductStatus = ProductStatus.ACTIVE
        protected set

    override fun guard() {
        if (price < 0) {
            throw IllegalArgumentException("상품 가격은 0 이상이어야 합니다.")
        }
    }

    fun toModel(): ProductModel = ProductModel(
        id = id,
        brandId = brandId,
        name = name,
        description = description,
        price = price,
        stockQuantity = stockQuantity,
        likeCount = likeCount,
        imageUrl = imageUrl,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

    fun updateFrom(model: ProductModel) {
        this.name = model.name
        this.description = model.description
        this.price = model.price
        this.stockQuantity = model.stockQuantity
        this.imageUrl = model.imageUrl
        this.status = model.status
        if (model.deletedAt != null) {
            this.deletedAt = model.deletedAt
        }
    }

    companion object {
        fun from(model: ProductModel): ProductJpaModel =
            ProductJpaModel(
                brandId = model.brandId,
                name = model.name,
                description = model.description,
                price = model.price,
                stockQuantity = model.stockQuantity,
                imageUrl = model.imageUrl,
            )
    }
}
