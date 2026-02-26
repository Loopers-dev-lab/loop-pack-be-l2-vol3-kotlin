package com.loopers.domain.product

import com.loopers.domain.BaseEntity
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.common.StockQuantity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "products",
    indexes = [Index(name = "idx_products_brand_id", columnList = "brand_id")],
)
class Product(
    name: String,
    description: String?,
    price: Money,
    likes: Int,
    stockQuantity: StockQuantity,
    brandId: Long,
) : BaseEntity() {

    var name: String = name
        protected set

    var description: String? = description
        protected set

    var price: Money = price
        protected set

    var likes: Int = likes
        protected set

    var stockQuantity: StockQuantity = stockQuantity
        protected set

    var brandId: Long = brandId
        protected set

    init {
        validateName(name)
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품 이름은 비어있을 수 없습니다.")
        }
    }

    fun increaseLikeCount() {
        this.likes += 1
    }

    fun decreaseLikeCount() {
        if (this.likes > 0) {
            this.likes -= 1
        }
    }

    fun deductStock(quantity: Quantity) {
        this.stockQuantity = this.stockQuantity - quantity
    }
}
