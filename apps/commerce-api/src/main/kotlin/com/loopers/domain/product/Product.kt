package com.loopers.domain.product

import com.loopers.domain.BaseEntity
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
    price: Long,
    likes: Int,
    stockQuantity: Int,
    brandId: Long,
) : BaseEntity() {

    var name: String = name
        protected set

    var description: String? = description
        protected set

    var price: Long = price
        protected set

    var likes: Int = likes
        protected set

    var stockQuantity: Int = stockQuantity
        protected set

    var brandId: Long = brandId
        protected set

    init {
        validateName(name)
        validatePrice(price)
        validateStockQuantity(stockQuantity)
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품 이름은 비어있을 수 없습니다.")
        }
    }

    private fun validatePrice(price: Long) {
        if (price < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품 가격은 0 이상이어야 합니다.")
        }
    }

    private fun validateStockQuantity(stockQuantity: Int) {
        if (stockQuantity < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고 수량은 0 이상이어야 합니다.")
        }
    }
}
