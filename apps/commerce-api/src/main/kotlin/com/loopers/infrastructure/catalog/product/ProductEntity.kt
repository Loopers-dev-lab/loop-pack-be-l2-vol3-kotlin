package com.loopers.infrastructure.catalog.product

import com.loopers.domain.BaseEntity
import com.loopers.domain.catalog.product.Product
import com.loopers.domain.catalog.product.ProductStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "products",
    indexes = [
        Index(name = "idx_products_brand_id", columnList = "brand_id"),
        Index(name = "idx_products_created_at", columnList = "created_at"),
        Index(name = "idx_products_price", columnList = "price"),
        Index(name = "idx_products_like_count", columnList = "like_count"),
    ]
)
class ProductEntity(
    brandId: Long,
    name: String,
    description: String,
    price: Int,
    stock: Int,
    likeCount: Int = 0,
    status: ProductStatus = ProductStatus.ACTIVE,
) : BaseEntity() {

    @Column(name = "brand_id", nullable = false)
    var brandId: Long = brandId
        protected set

    @Column(nullable = false)
    var name: String = name
        protected set

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String = description
        protected set

    @Column(nullable = false)
    var price: Int = price
        protected set

    @Column(nullable = false)
    var stock: Int = stock
        protected set

    @Column(name = "like_count", nullable = false)
    var likeCount: Int = likeCount
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ProductStatus = status
        protected set

    fun update(name: String, description: String, price: Int, stock: Int, status: ProductStatus) {
        this.name = name
        this.description = description
        this.price = price
        this.stock = stock
        this.status = status
    }

    fun updateLikeCount(likeCount: Int) {
        this.likeCount = likeCount
    }

    fun updateStock(stock: Int) {
        this.stock = stock
    }

    fun updateStatus(status: ProductStatus) {
        this.status = status
    }

    fun toDomain(): Product = Product(
        id = this.id,
        brandId = this.brandId,
        name = this.name,
        description = this.description,
        price = this.price,
        stock = this.stock,
        likeCount = this.likeCount,
        status = this.status,
    )

    companion object {
        fun from(product: Product): ProductEntity = ProductEntity(
            brandId = product.brandId,
            name = product.name,
            description = product.description,
            price = product.price,
            stock = product.stock,
            likeCount = product.likeCount,
            status = product.status,
        )
    }
}
