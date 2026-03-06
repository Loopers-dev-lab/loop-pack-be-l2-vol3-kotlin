package com.loopers.interfaces.admin.order

import com.loopers.domain.order.dto.AdminOrderInfo
import java.math.BigDecimal
import java.time.ZonedDateTime

object AdminOrderV1Dto {

    data class OrderResponse(
        val orderId: Long,
        val userId: Long,
        val orderDate: ZonedDateTime,
        val totalPrice: BigDecimal,
    ) {
        companion object {
            fun from(info: AdminOrderInfo): OrderResponse = OrderResponse(
                orderId = info.orderId,
                userId = info.userId,
                orderDate = info.orderDate,
                totalPrice = info.totalPrice,
            )
        }
    }

    data class OrderDetailResponse(
        val orderId: Long,
        val userId: Long,
        val orderDate: ZonedDateTime,
        val totalPrice: BigDecimal,
        val orderItems: List<OrderItemResponse>,
    ) {
        data class OrderItemResponse(
            val productId: Long,
            val productName: String,
            val quantity: Int,
            val price: BigDecimal,
            val subtotal: BigDecimal,
        )

        companion object {
            fun from(info: AdminOrderInfo): OrderDetailResponse = OrderDetailResponse(
                orderId = info.orderId,
                userId = info.userId,
                orderDate = info.orderDate,
                totalPrice = info.totalPrice,
                orderItems = info.orderItems.map {
                    OrderItemResponse(
                    productId = it.productId,
                    productName = it.productName,
                    quantity = it.quantity,
                    price = it.price,
                    subtotal = it.subtotal,
                )
                },
            )
        }
    }
}
