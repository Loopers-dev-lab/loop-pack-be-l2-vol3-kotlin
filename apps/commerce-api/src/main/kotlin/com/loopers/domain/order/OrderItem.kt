package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.domain.product.Product
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "order_items")
class OrderItem protected constructor(
    @Column(name = "order_id", nullable = false)
    val orderId: Long,
    @Column(name = "product_id", nullable = false)
    val productId: Long,
    @Column(name = "product_name", nullable = false, length = 200)
    val productName: String,
    @Column(nullable = false)
    val quantity: Int,
    @Column(nullable = false, precision = 19, scale = 2)
    val price: BigDecimal = BigDecimal.ZERO,
) : BaseEntity() {

    @Column(nullable = false, precision = 19, scale = 2)
    var discountAmount: BigDecimal = BigDecimal.ZERO
        protected set

    fun getSubtotal(): BigDecimal {
        return (price * BigDecimal(quantity.toLong())) - discountAmount
    }

    fun applyDiscountAmount(discount: BigDecimal) {
        if (discount < BigDecimal.ZERO) {
            throw CoreException(ErrorType.BAD_REQUEST, "할인액은 0 이상이어야 합니다")
        }
        this.discountAmount = discount
    }

    fun getItemAmount(): BigDecimal {
        return price * BigDecimal(quantity.toLong())
    }

    companion object {
        fun create(
            order: Order,
            product: Product,
            quantity: Int,
            price: BigDecimal,
        ): OrderItem {
            require(quantity > 0) { "수량은 0보다 커야 합니다" }
            require(price > BigDecimal.ZERO) { "가격은 0보다 커야 합니다" }

            return OrderItem(
                orderId = order.id,
                productId = product.id,
                productName = product.name,
                quantity = quantity,
                price = price,
            )
        }

        // 기존 방식 오버로드 (테스트 호환성 유지)
        fun create(
            orderId: Long,
            productId: Long,
            productName: String,
            quantity: Int,
            price: BigDecimal,
        ): OrderItem {
            require(quantity > 0) { "수량은 0보다 커야 합니다" }
            require(price > BigDecimal.ZERO) { "가격은 0보다 커야 합니다" }

            return OrderItem(
                orderId = orderId,
                productId = productId,
                productName = productName,
                quantity = quantity,
                price = price,
            )
        }
    }
}
