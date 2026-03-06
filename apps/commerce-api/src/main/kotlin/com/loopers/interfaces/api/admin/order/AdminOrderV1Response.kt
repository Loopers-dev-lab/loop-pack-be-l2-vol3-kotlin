package com.loopers.interfaces.api.admin.order

import com.loopers.application.admin.order.AdminOrderResult
import java.math.BigDecimal
import java.time.ZonedDateTime

class AdminOrderV1Response {

    data class Detail(
        val orderId: Long,
        val userId: Long,
        val status: String,
        val items: List<OrderItemDetail>,
        val totalAmount: BigDecimal,
        val discountAmount: BigDecimal,
        val finalAmount: BigDecimal,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: AdminOrderResult.Detail): Detail =
                Detail(
                    orderId = result.orderId,
                    userId = result.userId,
                    status = result.status,
                    items = result.items.map { OrderItemDetail.from(it) },
                    totalAmount = result.totalAmount,
                    discountAmount = result.discountAmount,
                    finalAmount = result.finalAmount,
                    createdAt = result.createdAt,
                )
        }
    }

    data class OrderItemDetail(
        val productId: Long,
        val productName: String,
        val brandName: String,
        val regularPrice: BigDecimal,
        val sellingPrice: BigDecimal,
        val thumbnailUrl: String?,
        val quantity: Int,
    ) {
        companion object {
            fun from(result: AdminOrderResult.OrderItemDetail): OrderItemDetail =
                OrderItemDetail(
                    productId = result.productId,
                    productName = result.productName,
                    brandName = result.brandName,
                    regularPrice = result.regularPrice,
                    sellingPrice = result.sellingPrice,
                    thumbnailUrl = result.thumbnailUrl,
                    quantity = result.quantity,
                )
        }
    }

    data class ListItem(
        val orderId: Long,
        val userId: Long,
        val status: String,
        val orderSummary: String,
        val itemCount: Int,
        val totalAmount: BigDecimal,
        val discountAmount: BigDecimal,
        val finalAmount: BigDecimal,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: AdminOrderResult.ListItem): ListItem =
                ListItem(
                    orderId = result.orderId,
                    userId = result.userId,
                    status = result.status,
                    orderSummary = result.orderSummary,
                    itemCount = result.itemCount,
                    totalAmount = result.totalAmount,
                    discountAmount = result.discountAmount,
                    finalAmount = result.finalAmount,
                    createdAt = result.createdAt,
                )
        }
    }
}
