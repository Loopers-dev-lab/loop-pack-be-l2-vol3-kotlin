package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "order_items")
class OrderItem protected constructor(
    @Column(name = "order_id", nullable = false)
    val orderId: Long = 0L,
    @Column(name = "product_id", nullable = false)
    val productId: Long = 0L,
    @Column(nullable = false)
    val quantity: Int = 0,
    @Column(nullable = false, precision = 19, scale = 2)
    val price: BigDecimal = BigDecimal.ZERO,
    @Column(name = "product_name", nullable = false, length = 200)
    val productName: String = "",
) : BaseEntity() {

    fun getSubtotal(): BigDecimal = price.multiply(BigDecimal(quantity))

    companion object {
        fun create(
            orderId: Long = 0L,
            productId: Long,
            quantity: Int,
            price: BigDecimal,
            productName: String,
        ): OrderItem {
            return OrderItem(
                orderId = orderId,
                productId = productId,
                quantity = quantity,
                price = price,
                productName = productName,
            )
        }
    }
}
