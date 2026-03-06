package com.loopers.interfaces.api.user.order

import com.loopers.application.user.order.OrderResult
import java.math.BigDecimal
import java.time.ZonedDateTime

class UserOrderV1Response {

    data class Created(
        val orderId: Long,
        val status: String,
        val discountAmount: BigDecimal,
        val finalAmount: BigDecimal,
    ) {
        companion object {
            fun from(result: OrderResult.Created): Created =
                Created(
                    orderId = result.orderId,
                    status = result.status,
                    discountAmount = result.discountAmount,
                    finalAmount = result.finalAmount,
                )
        }
    }

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
            fun from(result: OrderResult.Detail): Detail =
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
            fun from(result: OrderResult.OrderItemDetail): OrderItemDetail =
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
        val status: String,
        val orderSummary: String,
        val itemCount: Int,
        val totalAmount: BigDecimal,
        val discountAmount: BigDecimal,
        val finalAmount: BigDecimal,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: OrderResult.ListItem): ListItem =
                ListItem(
                    orderId = result.orderId,
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
