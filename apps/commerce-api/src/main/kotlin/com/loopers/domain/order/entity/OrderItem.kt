package com.loopers.domain.order.entity

import com.loopers.domain.BaseEntity
import com.loopers.domain.order.OrderProductInfo
import com.loopers.domain.order.vo.Quantity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "order_items")
class OrderItem private constructor(
    refProductId: Long,
    productName: String,
    productPrice: BigDecimal,
    quantity: Int,
) : BaseEntity() {

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

    init {
        guard()
    }

    override fun guard() {
        Quantity(quantity)
    }

    companion object {
        fun create(product: OrderProductInfo, quantity: Int): OrderItem {
            return OrderItem(
                refProductId = product.id,
                productName = product.name,
                productPrice = product.price,
                quantity = quantity,
            )
        }
    }
}
