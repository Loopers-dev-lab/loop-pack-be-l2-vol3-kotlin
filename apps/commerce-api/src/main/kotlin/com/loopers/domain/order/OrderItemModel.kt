package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "order_item")
class OrderItemModel(
    order: OrderModel,
    productId: Long,
    productName: String,
    brandName: String,
    price: Long,
    quantity: Int,
) : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    var order: OrderModel = order
        protected set

    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Column(name = "product_name", nullable = false, length = 200)
    var productName: String = productName
        protected set

    @Column(name = "brand_name", nullable = false, length = 100)
    var brandName: String = brandName
        protected set

    @Column(nullable = false)
    var price: Long = price
        protected set

    @Column(nullable = false)
    var quantity: Int = quantity
        protected set

    @Column(name = "sub_total", nullable = false)
    var subTotal: Long = price * quantity
        protected set

    init {
        validateQuantity(quantity)
    }

    private fun validateQuantity(quantity: Int) {
        if (quantity < 1 || quantity > 99) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 수량은 1개 이상 99개 이하여야 합니다.")
        }
    }
}
