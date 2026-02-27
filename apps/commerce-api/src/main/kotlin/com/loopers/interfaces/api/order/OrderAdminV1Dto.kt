package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderItemResult
import com.loopers.application.order.OrderResult
import com.loopers.domain.Money
import java.time.ZonedDateTime

class OrderAdminV1Dto {

    data class OrderAdminResponse(
        val id: Long,
        val userId: Long,
        val orderNumber: String,
        val totalAmount: Money,
        val orderStatus: String,
        val items: List<OrderItemAdminResponse>,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: OrderResult): OrderAdminResponse {
                return OrderAdminResponse(
                    id = result.id,
                    userId = result.userId,
                    orderNumber = result.orderNumber,
                    totalAmount = result.totalAmount,
                    orderStatus = result.orderStatus,
                    items = result.items.map { OrderItemAdminResponse.from(it) },
                    createdAt = result.createdAt,
                )
            }
        }
    }

    data class OrderItemAdminResponse(
        val id: Long,
        val productId: Long,
        val quantity: Int,
        val itemStatus: String,
        val productName: String,
        val brandName: String,
        val brandId: Long,
        val imageUrl: String?,
        val originalPrice: Money,
        val discountAmount: Money,
        val finalPrice: Money,
    ) {
        companion object {
            fun from(result: OrderItemResult): OrderItemAdminResponse {
                return OrderItemAdminResponse(
                    id = result.id,
                    productId = result.productId,
                    quantity = result.quantity,
                    itemStatus = result.itemStatus,
                    productName = result.productName,
                    brandName = result.brandName,
                    brandId = result.brandId,
                    imageUrl = result.imageUrl,
                    originalPrice = result.originalPrice,
                    discountAmount = result.discountAmount,
                    finalPrice = result.finalPrice,
                )
            }
        }
    }
}
