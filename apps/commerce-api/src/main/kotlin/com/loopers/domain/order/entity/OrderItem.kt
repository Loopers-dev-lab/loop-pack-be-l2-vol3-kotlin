package com.loopers.domain.order.entity

import com.loopers.domain.BaseEntity
import com.loopers.domain.order.OrderProductInfo
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "order_items")
class OrderItem private constructor(
    refOrderId: Long,
    refProductId: Long,
    productName: String,
    productPrice: BigDecimal,
    quantity: Int,
) : BaseEntity() {

    @Column(name = "ref_order_id", nullable = false)
    var refOrderId: Long = refOrderId
        protected set

    @Column(name = "ref_product_id", nullable = false)
    var refProductId: Long = refProductId
        protected set

    @Column(name = "product_name", nullable = false)
    var productName: String = productName
        protected set

    @Column(name = "product_price", nullable = false)
    var productPrice: BigDecimal = productPrice
        protected set

    @Column(name = "quantity", nullable = false)
    var quantity: Int = quantity
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: ItemStatus = ItemStatus.ACTIVE
        protected set

    init {
        if (quantity < 1) {
            throw CoreException(ErrorType.BAD_REQUEST, "수량은 1 이상이어야 합니다.")
        }
    }

    fun cancel() {
        if (status == ItemStatus.CANCELLED) {
            throw CoreException(ErrorType.BAD_REQUEST, "이미 취소된 주문 아이템입니다.")
        }
        status = ItemStatus.CANCELLED
    }

    enum class ItemStatus {
        ACTIVE,
        CANCELLED,
    }

    companion object {
        fun create(product: OrderProductInfo, quantity: Int, orderId: Long): OrderItem {
            return OrderItem(
                refOrderId = orderId,
                refProductId = product.id,
                productName = product.name,
                productPrice = product.price,
                quantity = quantity,
            )
        }
    }
}
