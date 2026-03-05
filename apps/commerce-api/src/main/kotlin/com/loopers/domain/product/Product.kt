package com.loopers.domain.product

import com.loopers.domain.BaseEntity
import com.loopers.domain.common.LikeCount
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
    likes: LikeCount,
    stockQuantity: StockQuantity,
    brandId: Long,
) : BaseEntity() {

    var name: String = name
        protected set

    var description: String? = description
        protected set

    var price: Money = price
        protected set

    var likes: LikeCount = likes
        protected set

    var stockQuantity: StockQuantity = stockQuantity
        protected set

    var brandId: Long = brandId
        protected set

    init {
        validateName(name)
        validatePrice(price)
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품 이름은 비어있을 수 없습니다.")
        }
    }

    private fun validatePrice(price: Money) {
        if (price.value <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "가격은 0보다 커야 합니다.")
        }
    }

    fun validateBrandChange(brandId: Long) {
        if (this.brandId != brandId) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품의 브랜드는 수정할 수 없습니다.")
        }
    }

    fun update(name: String, description: String?, price: Money, stockQuantity: StockQuantity) {
        validateName(name)
        validatePrice(price)
        this.name = name
        this.description = description
        this.price = price
        this.stockQuantity = stockQuantity
    }

    fun deductStock(quantity: Quantity) {
        this.stockQuantity = this.stockQuantity - quantity
    }

    companion object {
        fun create(
            name: String,
            description: String?,
            price: Money,
            stockQuantity: StockQuantity,
            brandId: Long,
        ): Product {
            return Product(
                name = name,
                description = description,
                price = price,
                likes = LikeCount.of(0),
                stockQuantity = stockQuantity,
                brandId = brandId,
            )
        }
    }
}
