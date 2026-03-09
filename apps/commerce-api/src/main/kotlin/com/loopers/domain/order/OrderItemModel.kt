package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "order_items")
class OrderItemModel(
    orderId: Long,
    productId: Long,
    productName: String,
    quantity: Int,
    price: BigDecimal,
) : BaseEntity() {
    @Column(name = "order_id", nullable = false)
    var orderId: Long = orderId
        protected set

    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Column(name = "product_name", nullable = false)
    var productName: String = productName
        protected set

    @Column(nullable = false)
    var quantity: Int = quantity
        protected set

    @Column(nullable = false, precision = 10, scale = 2)
    var price: BigDecimal = price
        protected set

    init {
        validateProductName(productName)
        validateQuantity(quantity)
        validatePrice(price)
    }

    private fun validateProductName(productName: String) {
        if (productName.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품명은 비어있을 수 없습니다.")
        }
    }

    private fun validateQuantity(quantity: Int) {
        if (quantity <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 수량은 0보다 커야 합니다.")
        }
    }

    private fun validatePrice(price: BigDecimal) {
        if (price <= BigDecimal.ZERO) {
            throw CoreException(ErrorType.BAD_REQUEST, "가격은 0보다 커야 합니다.")
        }
    }
}
