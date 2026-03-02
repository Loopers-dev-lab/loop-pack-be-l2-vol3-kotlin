package com.loopers.application.order

import com.loopers.domain.order.OrderLine
import com.loopers.domain.order.Quantity

class OrderCommand {

    data class Create(
        val userId: Long,
        val items: List<OrderLineItem>,
    ) {
        data class OrderLineItem(
            val productId: Long,
            val quantity: Int,
        )

        fun toOrderLines(): List<OrderLine> = items.map {
            OrderLine(productId = it.productId, quantity = Quantity(it.quantity))
        }
    }
}
