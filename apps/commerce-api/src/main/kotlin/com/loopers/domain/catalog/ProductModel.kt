package com.loopers.domain.catalog

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal

@Entity
@Table(name = "products")
class ProductModel(
    brandId: Long,
    name: String,
    quantity: Int,
    price: BigDecimal,
) : BaseEntity() {
    @Column(name = "brand_id", nullable = false)
    var brandId: Long = brandId
        protected set

    var name: String = name
        protected set

    var quantity: Int = quantity
        protected set

    @Column(precision = 10, scale = 2)
    var price: BigDecimal = price
        protected set

    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0
        protected set

    @Version
    var version: Long = 0
        protected set

    init {
        validateName(name)
        validatePrice(price)
        validateQuantity(quantity)
    }

    fun decreaseStock(quantity: Int) {
        if (quantity <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "차감 수량은 0보다 커야 합니다.")
        }
        if (this.quantity < quantity) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.")
        }
        this.quantity -= quantity
    }

    fun increaseStock(quantity: Int) {
        if (quantity <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "복구 수량은 0보다 커야 합니다.")
        }
        this.quantity += quantity
    }

    fun update(
        newName: String,
        newQuantity: Int,
        newPrice: BigDecimal,
    ) {
        validateName(newName)
        validatePrice(newPrice)
        validateQuantity(newQuantity)
        this.name = newName
        this.quantity = newQuantity
        this.price = newPrice
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품명은 비어있을 수 없습니다.")
        }
    }

    private fun validatePrice(price: BigDecimal) {
        if (price <= BigDecimal.ZERO) {
            throw CoreException(ErrorType.BAD_REQUEST, "가격은 0보다 커야 합니다.")
        }
    }

    private fun validateQuantity(quantity: Int) {
        if (quantity < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "수량은 0 이상이어야 합니다.")
        }
    }
}
