package com.loopers.domain.product

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "products")
class Product(
    @Column(name = "brand_id", nullable = false)
    val brandId: Long,
    name: String,
    description: String,
    price: Long,
    stockQuantity: Int,
) : BaseEntity() {
    var name: String = name
        protected set

    var description: String = description
        protected set

    var price: Long = price
        protected set

    @Column(name = "stock_quantity", nullable = false)
    var stockQuantity: Int = stockQuantity
        protected set

    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0
        protected set

    fun decreaseStock(quantity: Int) {
        if (stockQuantity < quantity) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.")
        }
        stockQuantity -= quantity
    }

    fun increaseLikeCount() {
        likeCount++
    }

    fun decreaseLikeCount() {
        if (likeCount > 0) likeCount--
    }

    fun update(name: String, description: String, price: Long, stockQuantity: Int) {
        this.name = name
        this.description = description
        this.price = price
        this.stockQuantity = stockQuantity
    }
}
