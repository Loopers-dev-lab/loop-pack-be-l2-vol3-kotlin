package com.loopers.application.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import java.time.ZonedDateTime

data class OrderInfo(
    val id: Long,
    val userId: Long,
    val status: String,
    val totalAmount: Long,
    val orderedAt: ZonedDateTime,
    val items: List<OrderItemInfo>,
) {
    companion object {
        fun from(order: Order): OrderInfo {
            val id = requireNotNull(order.persistenceId) {
                "Order.persistenceId가 null입니다. 저장된 Order만 매핑 가능합니다."
            }
            return OrderInfo(
                id = id,
                userId = order.refUserId,
                status = order.status.name,
                totalAmount = order.totalAmount.amount,
                orderedAt = order.orderedAt,
                items = order.items.map { OrderItemInfo.from(it) },
            )
        }
    }
}

data class OrderItemInfo(
    val id: Long,
    val productId: Long,
    val productName: String,
    val brandName: String,
    val price: Long,
    val quantity: Int,
) {
    companion object {
        fun from(item: OrderItem): OrderItemInfo {
            val id = requireNotNull(item.persistenceId) {
                "OrderItem.persistenceId가 null입니다."
            }
            return OrderItemInfo(
                id = id,
                productId = item.refProductId,
                productName = item.productName,
                brandName = item.brandName,
                price = item.price.amount,
                quantity = item.quantity,
            )
        }
    }
}
