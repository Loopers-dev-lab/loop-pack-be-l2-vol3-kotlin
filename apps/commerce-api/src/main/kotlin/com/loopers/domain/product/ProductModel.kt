package com.loopers.domain.product

import com.loopers.domain.BaseEntity
import com.loopers.domain.product.vo.ProductDescription
import com.loopers.domain.product.vo.ProductName
import com.loopers.domain.product.vo.StockQuantity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "product")
class ProductModel(
    brandId: Long,
    name: ProductName,
    description: ProductDescription,
    price: Long,
    stockQuantity: StockQuantity,
    imageUrl: String,
) : BaseEntity() {
    @Column(name = "brand_id", nullable = false)
    var brandId: Long = brandId
        protected set

    @Column(name = "name", nullable = false)
    var name: String = name.value
        protected set

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    var description: String = description.value
        protected set

    @Column(name = "price", nullable = false)
    var price: Long = price
        protected set

    @Column(name = "stock_quantity", nullable = false)
    var stockQuantity: Int = stockQuantity.value
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
            throw CoreException(ErrorType.BAD_REQUEST, "상품 가격은 0 이상이어야 합니다.")
        }
    }

    fun deductStock(quantity: Int) {
        if (quantity <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "차감 수량은 1 이상이어야 합니다.")
        }
        if (stockQuantity < quantity) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.")
        }
        stockQuantity -= quantity
    }

    fun update(
        name: ProductName,
        description: ProductDescription,
        price: Long,
        stockQuantity: StockQuantity,
        imageUrl: String,
    ) {
        this.name = name.value
        this.description = description.value
        this.price = price
        this.stockQuantity = stockQuantity.value
        this.imageUrl = imageUrl
    }

    override fun delete() {
        this.status = ProductStatus.DELETED
        super.delete()
    }

    fun isDeleted(): Boolean = status == ProductStatus.DELETED
}
