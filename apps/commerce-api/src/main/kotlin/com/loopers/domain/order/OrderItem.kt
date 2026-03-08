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
    var orderId: Long,
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

    @Column(nullable = false)
    private var discountApplied: Boolean = false

    internal fun setOrderId(newOrderId: Long) {
        this.orderId = newOrderId
    }

    fun getSubtotal(): BigDecimal {
        return (price * BigDecimal(quantity.toLong())) - discountAmount
    }

    fun applyDiscountAmount(discount: BigDecimal) {
        if (discount < BigDecimal.ZERO) {
            throw CoreException(ErrorType.BAD_REQUEST, "할인액은 0 이상이어야 합니다")
        }
        if (discountApplied) {
            throw CoreException(ErrorType.BAD_REQUEST, "이미 할인이 적용된 항목입니다")
        }
        if (discount > getItemAmount()) {
            throw CoreException(ErrorType.BAD_REQUEST, "할인액은 항목 금액을 초과할 수 없습니다")
        }
        this.discountAmount = discount
        this.discountApplied = true
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
            if (quantity <= 0) {
                throw CoreException(ErrorType.BAD_REQUEST, "수량은 0보다 커야 합니다")
            }
            if (price <= BigDecimal.ZERO) {
                throw CoreException(ErrorType.BAD_REQUEST, "가격은 0보다 커야 합니다")
            }

            // Precondition: Order must be persisted before calling addItem()
            // OrderService.createOrder()는 Order.save() 후 addItem()을 호출하므로 order.id는 올바른 값
            // createWithItems() 사용 시에는 저장 후 setOrderItemIds()를 호출해야 함
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
            if (quantity <= 0) {
                throw CoreException(ErrorType.BAD_REQUEST, "수량은 0보다 커야 합니다")
            }
            if (price <= BigDecimal.ZERO) {
                throw CoreException(ErrorType.BAD_REQUEST, "가격은 0보다 커야 합니다")
            }

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
