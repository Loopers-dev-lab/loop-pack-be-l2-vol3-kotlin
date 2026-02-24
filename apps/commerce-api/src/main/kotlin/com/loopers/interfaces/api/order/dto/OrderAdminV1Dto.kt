package com.loopers.interfaces.api.order.dto

import com.loopers.application.order.OrderInfo
import com.loopers.application.order.OrderItemInfo
import java.math.BigDecimal

class OrderAdminV1Dto {

    enum class OrderStatusDto { CREATED, PAID, CANCELLED, FAILED }
    enum class OrderItemStatusDto { ACTIVE, CANCELLED }

    data class OrderAdminResponse(
        val id: Long,
        val userId: Long,
        val status: OrderStatusDto,
        val totalPrice: BigDecimal,
        val items: List<OrderItemAdminResponse>,
    ) {
        companion object {
            fun from(info: OrderInfo): OrderAdminResponse {
                return OrderAdminResponse(
                    id = info.id,
                    userId = info.userId,
                    status = OrderStatusDto.valueOf(info.status),
                    totalPrice = info.totalPrice,
                    items = info.items.map { OrderItemAdminResponse.from(it) },
                )
            }
        }
    }

    data class OrderItemAdminResponse(
        val id: Long,
        val productId: Long,
        val productName: String,
        val productPrice: BigDecimal,
        val quantity: Int,
    ) {
        companion object {
            fun from(item: OrderItemInfo): OrderItemAdminResponse {
                return OrderItemAdminResponse(
                    id = item.id,
                    productId = item.productId,
                    productName = item.productName,
                    productPrice = item.productPrice,
                    quantity = item.quantity,
                )
            }
        }
    }
}
