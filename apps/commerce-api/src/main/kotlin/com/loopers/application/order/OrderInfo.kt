package com.loopers.application.order

import com.loopers.domain.order.OrderItemModel
import com.loopers.domain.order.OrderModel
import java.time.ZonedDateTime

data class OrderInfo(
    val id: Long,
    val userId: Long,
    val totalAmount: Long,
    val status: String,
    val items: List<OrderItemInfo>,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
) {
    data class OrderItemInfo(
        val id: Long,
        val productId: Long,
        val brandId: Long,
        val productName: String,
        val imageUrl: String,
        val unitPrice: Long,
        val quantity: Long,
        val subTotal: Long,
    )

    companion object {
        fun from(order: OrderModel, items: List<OrderItemModel>): OrderInfo =
            OrderInfo(
                id = order.id,
                userId = order.userId,
                totalAmount = order.totalAmount.value,
                status = order.status.name,
                items = items.map { item ->
                    OrderItemInfo(
                        id = item.id,
                        productId = item.productId,
                        brandId = item.brandId,
                        productName = item.productName.value,
                        imageUrl = item.imageUrl.value,
                        unitPrice = item.unitPrice.value,
                        quantity = item.quantity.value,
                        subTotal = item.subTotal,
                    )
                },
                createdAt = order.createdAt,
                updatedAt = order.updatedAt,
            )
    }
}
