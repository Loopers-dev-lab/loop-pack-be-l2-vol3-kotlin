package com.loopers.domain.product

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "products")
class Product(
    brandId: Long,
    name: String,
    description: String? = null,
    price: Long,
    stockQuantity: Int,
    displayYn: Boolean = true,
    imageUrl: String? = null,
) : BaseEntity() {

    @Column(name = "brand_id", nullable = false)
    var brandId: Long = brandId
        protected set

    @Column(nullable = false)
    var name: String = name
        protected set

    @Column
    var description: String? = description
        protected set

    @Column(nullable = false)
    var price: Long = price
        protected set

    @Column(name = "stock_quantity", nullable = false)
    var stockQuantity: Int = stockQuantity
        protected set

    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ProductStatus = ProductStatus.ACTIVE
        protected set

    @Column(name = "display_yn", nullable = false)
    var displayYn: Boolean = displayYn
        protected set

    @Column(name = "image_url")
    var imageUrl: String? = imageUrl
        protected set

    init {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품명은 필수입니다.")
        }
        if (price < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품 가격은 0 이상이어야 합니다.")
        }
        if (stockQuantity < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고 수량은 0 이상이어야 합니다.")
        }
    }

    fun update(
        name: String,
        description: String?,
        price: Long,
        stockQuantity: Int,
        status: ProductStatus,
        displayYn: Boolean,
        imageUrl: String?,
    ) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품명은 필수입니다.")
        }
        if (price < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품 가격은 0 이상이어야 합니다.")
        }
        if (stockQuantity < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고 수량은 0 이상이어야 합니다.")
        }
        this.name = name
        this.description = description
        this.price = price
        this.stockQuantity = stockQuantity
        this.status = status
        this.displayYn = displayYn
        this.imageUrl = imageUrl
    }

    fun decreaseStock(quantity: Int) {
        if (this.stockQuantity < quantity) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품의 재고가 부족합니다.")
        }
        this.stockQuantity -= quantity
    }

    fun increaseStock(quantity: Int) {
        this.stockQuantity += quantity
    }

    fun increaseLikeCount() {
        this.likeCount++
    }

    fun decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--
        }
    }

    fun isOrderable(): Boolean {
        return status == ProductStatus.ACTIVE && displayYn && stockQuantity > 0
    }

    fun softDelete() {
        this.status = ProductStatus.DELETED
        delete()
    }
}
