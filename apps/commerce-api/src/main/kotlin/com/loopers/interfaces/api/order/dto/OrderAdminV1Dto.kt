package com.loopers.interfaces.api.order.dto

import com.loopers.application.order.OrderInfo
import com.loopers.application.order.OrderItemInfo
import java.math.BigDecimal

class OrderAdminV1Dto {

    data class OrderAdminResponse(
        val id: Long,
        val userId: Long,
        val status: String,
        val originalPrice: BigDecimal,
        val discountAmount: BigDecimal,
        val totalPrice: BigDecimal,
        val couponId: Long?,
        val items: List<OrderItemAdminResponse>,
    ) {
        companion object {
            fun from(info: OrderInfo): OrderAdminResponse {
                return OrderAdminResponse(
                    id = info.id,
                    userId = info.userId,
                    status = info.status,
                    originalPrice = info.originalPrice,
                    discountAmount = info.discountAmount,
                    totalPrice = info.totalPrice,
                    couponId = info.couponId,
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
