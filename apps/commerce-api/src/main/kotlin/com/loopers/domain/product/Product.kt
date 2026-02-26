package com.loopers.domain.product

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ProductErrorCode
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "products")
class Product private constructor(
    brandId: Long,
    name: String,
    description: String,
    price: Money,
    stock: Stock,
    imageUrl: String,
    likeCount: Long = 0,
    deletedAt: ZonedDateTime? = null,
) : BaseEntity() {

    @Column(name = "brand_id", nullable = false)
    var brandId: Long = brandId
        protected set

    @Column(nullable = false, length = 100)
    var name: String = name
        protected set

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String = description
        protected set

    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "price", nullable = false))
    var price: Money = price
        protected set

    @Embedded
    @AttributeOverride(name = "quantity", column = Column(name = "stock", nullable = false))
    var stock: Stock = stock
        protected set

    @Column(name = "image_url", nullable = false)
    var imageUrl: String = imageUrl
        protected set

    @Column(name = "like_count", nullable = false)
    var likeCount: Long = likeCount
        protected set

    @Column(name = "deleted_at")
    var deletedAt: ZonedDateTime? = deletedAt
        protected set

    fun isDeleted(): Boolean = deletedAt != null

    fun isAvailable(): Boolean = !isDeleted() && stock.quantity > 0

    fun update(
        name: String,
        description: String,
        price: Money,
        stock: Stock,
        imageUrl: String,
    ) {
        validateName(name)
        this.name = name.trim()
        this.description = description
        this.price = price
        this.stock = stock
        this.imageUrl = imageUrl
    }

    fun delete() {
        this.deletedAt = ZonedDateTime.now()
    }

    fun decreaseStock(quantity: Int) {
        this.stock = stock.deduct(quantity)
    }

    fun increaseLikeCount() {
        this.likeCount++
    }

    fun decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--
        }
    }

    companion object {
        private const val NAME_MIN_LENGTH = 1
        private const val NAME_MAX_LENGTH = 100

        fun create(
            brandId: Long,
            name: String,
            description: String,
            price: Money,
            stock: Stock,
            imageUrl: String,
        ): Product {
            validateName(name)
            return Product(
                brandId = brandId,
                name = name.trim(),
                description = description,
                price = price,
                stock = stock,
                imageUrl = imageUrl,
            )
        }

        private fun validateName(name: String) {
            val trimmed = name.trim()
            if (trimmed.length < NAME_MIN_LENGTH || trimmed.length > NAME_MAX_LENGTH) {
                throw CoreException(ProductErrorCode.INVALID_PRODUCT_NAME)
            }
        }
    }
}
